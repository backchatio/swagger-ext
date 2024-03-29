package mojolly.swagger.runtime

import java.net.URI
import java.util.Locale.ENGLISH
import java.nio.charset.Charset
import java.io.IOException

import scalax.io.{Codec => Codecx, Resource}
import io.Codec
import collection.JavaConversions._

import com.ning.http.client._
import util.control.Exception._
import net.liftweb.json._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import java.lang.reflect.Modifier
import mojolly.inflector.InflectorImports._

trait ApiAuth {
  def headers: Map[String, String]
}

trait ApiError

class JsonParseError() extends ApiError
class IoError() extends ApiError

trait Api {
  protected def client: ApiClient

  abstract class ApiOperation[T](val method: String, val pathPattern: String) {
    def queryParams: Iterable[(String, String)]
    def headerParams: Map[String, String]
    def pathParams: Map[String, String]
    def path = pathPattern
  }

  def value2params[T](key: String, value: T)(implicit mf: Manifest[T]): Iterable[(String, String)] = mf.erasure match {
    case x if x == classOf[String] => List((key, value.toString))
    case x if x == classOf[Boolean] => List((key, value.toString))
    case x if classOf[Product].isAssignableFrom(x) => product2params(key, value.asInstanceOf[Product])
    case x => object2params(key, value)
  }

  def object2params[T](key: String, value: T)(implicit mf: Manifest[T]): Iterable[(String, String)] = Nil

  def product2params[T <: Product](key: String, value: T): Iterable[(String, String)] = {
    val r = value.getClass.getDeclaredFields.toList filter (f => (f.getModifiers & Modifier.PRIVATE) != 0) map (_.getName)
    val f = value.productIterator.toList
    (r, f).zipped.toList collect {
      case (k, v) if v != null => (k.underscore, v.toString)
    }
  }

  object Params {
    trait Param {
      def values: Iterable[(String, String)]
    }
    case object EmptyParam extends Param {
      def values: Iterable[(String, String)] = Nil
    }
    case class CParam(values: Iterable[(String, String)]) extends Param
    case class LParam(params: List[Param]) extends Param {
      def values: Iterable[(String, String)] = params flatMap (_.values)
    }

    implicit def option2param[T](p: (String, Option[T]))(implicit mf: Manifest[T]): Param = {
      p._2 map (v => {
        CParam(value2params[T](p._1, v))
      }) getOrElse EmptyParam
    }

    implicit def any2param[T](p: (String, T))(implicit mf: Manifest[T]): Param = {
      Option(p._2) map (v => {
        CParam(value2params[T](p._1, v))
      }) getOrElse EmptyParam
    }

    implicit def list2param[T](p: (String, List[T]))(implicit mf: Manifest[T]): Param = {
      if (p._2 == null) EmptyParam else {
        LParam(p._2 map (v => any2param[T](p._1, v)))
      }
    }

    def apply(params: Param*): Iterable[(String,  String)] = (params filterNot (p => p.values.isEmpty) flatMap (p => p.values))
  }

  implicit def apiOperation2result[T](op: ApiOperation[T])(implicit auth: ApiAuth, mf: scala.reflect.Manifest[T]): Either[ApiError, T] = {
    val path = (op.path /: op.pathParams) { (path, param) => path.replace(("{%s}" format param._1), param._2) }
    client.submit(op.method, path, op.queryParams, op.headerParams, true)
  }
}

trait ApiClient {
  private val clientConfig = new AsyncHttpClientConfig.Builder().setFollowRedirects(false).build()
  private val underlying = new AsyncHttpClient(clientConfig)

  object DateTimeSerializer extends Serializer[DateTime] {
    private val Target = classOf[DateTime]
    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), DateTime] = {
      case (TypeInfo(Target, _), json) =>
        val opt = json match {
          case json: JString =>
            (catching(classOf[IllegalArgumentException])).opt(Iso8601Date.parseDateTime(json.s))
          case value => None
        }
        opt getOrElse { throw new MappingException("Can't convert " + json + " to DateTime") }
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case d: DateTime => JString(d.toString(Iso8601Date))
    }
  }

  implicit val formats = Serialization.formats(NoTypeHints) + DateTimeSerializer

  lazy val Iso8601Date = ISODateTimeFormat.dateTime.withZone(DateTimeZone.UTC)

  def host: String
  def port: Int
  def apiPath: String

  def close() {
    underlying.close()
  }

  class RichJValue(jvalue: JValue) {
    def camelizeKeys = FormattedJson.rewriteJsonAST(jvalue, true)
    def snakizeKeys = FormattedJson.rewriteJsonAST(jvalue, false)
  }

  implicit def richValue(jvalue: JValue) = new RichJValue(jvalue)

  def submit[T](method: String, path: String,
                queryParams: Iterable[(String, String)],
                headers: Map[String, String], authRequired: Boolean)(implicit auth: ApiAuth, mf: scala.reflect.Manifest[T]): Either[ApiError, T] = {
    val u = URI.create(path)
    val reqUri = if (u.isAbsolute) u else new URI("http", null, host, port, apiPath + u.getPath, u.getQuery, u.getFragment)
    val req = (requestFactory(method)
      andThen (addHeaders(headers) _)
      andThen (addAuth(auth, authRequired) _)
      andThen (addParameters(method, queryParams) _))(reqUri.toASCIIString)
    parseResponse(method, req.execute(async).get)
  }

  def parseResponse[T](method: String, resp: ClientResponse)(implicit mf: scala.reflect.Manifest[T]): Either[ApiError, T] = {
    if (method == "DELETE" && resp.statusCode == 204)
      Right(().asInstanceOf[T])
    else
      withResponse(resp) { json =>
        json.extract[T]
      }
  }

  def withResponse[T](response: ClientResponse)(f: JValue => T)(implicit mf: scala.reflect.Manifest[T]): Either[ApiError, T] = {
    val camelized = ((parseOpt(response.body) map (j => j.camelizeKeys)))
    (camelized toRight (new JsonParseError())).right flatMap {
      case JObject(JField("data", data) :: JField("errors", JArray(errors)) :: Nil) =>
        if (errors.isEmpty)
          (catching(classOf[IOException])).either { f(data) }.left map (_ => new IoError)
        else {
          Left(new ApiError {})
        }
      case JObject(JField("errors", JArray(errors)) :: Nil) if mf.erasure == classOf[Unit] =>
        if (errors.isEmpty)
          Right(().asInstanceOf[T])
        else
          Left(new ApiError {})
      case _ => Left(new JsonParseError())
    }
  }

  private def requestFactory(method: String): String ⇒ AsyncHttpClient#BoundRequestBuilder = {
    method.toUpperCase(ENGLISH) match {
      case "GET"     ⇒ underlying.prepareGet _
      case "POST"    ⇒ underlying.preparePost _
      case "PUT"     ⇒ underlying.preparePut _
      case "DELETE"  ⇒ underlying.prepareDelete _
    }
  }

  private def addParameters(method: String, params: Iterable[(String, String)])(req: AsyncHttpClient#BoundRequestBuilder) = {
    method.toUpperCase(ENGLISH) match {
      case "GET" | "DELETE" ⇒ params foreach { case (k, v) ⇒ req addQueryParameter (k, v) }
      case "PUT" | "POST"   | "PATCH"            ⇒ {
        params foreach { case (k, v) ⇒ req addParameter (k, v) }
      }
      case _                                     ⇒ // we don't care, carry on
    }
    req
  }

  private def addHeaders(headers: Map[String, String])(req: AsyncHttpClient#BoundRequestBuilder) = {
    headers foreach { case (k, v) => req.setHeader(k, v) }
    req
  }

  private def addAuth(auth: ApiAuth, required: Boolean)(req: AsyncHttpClient#BoundRequestBuilder) = {
    if (required) auth.headers foreach { case (k, v) => req.setHeader(k, v) }
    req
  }

  private def async = new AsyncCompletionHandler[ClientResponse] {

    override def onThrowable(t: Throwable) {
      t.printStackTrace()
    }

    def onCompleted(response: Response) = new ClientResponse(response)
  }

  case class ResponseStatus(code: Int, message: String) {
    def line = {
      val buf = new StringBuilder(message.length + 5);
      buf.append(code)
      buf.append(' ')
      buf.append(message)
      buf.toString()
    }
  }

  class ClientResponse(response: Response) {
    private var _body: String = null

    val inputStream = response.getResponseBodyAsStream

    def body = {
      if (_body == null) _body = Resource.fromInputStream(inputStream).slurpString(Codecx(nioCharset))
      _body
    }

    private def nioCharset = charset map Charset.forName getOrElse Codec.UTF8

    val headers: Map[String, String] = (response.getHeaders.keySet() map { k => k -> response.getHeaders(k).mkString("; ")}).toMap

    val status = ResponseStatus(response.getStatusCode, response.getStatusText)

    def charset: Option[String] =
      for {
        ct <- headers.get("Content-Type")
        charset <- ct.split(";").drop(1).headOption
      } yield { charset.toUpperCase.replace("CHARSET=", "").trim }

    def statusCode = status.code
    def statusText = status.line
  }

  object FormattedJson {
    def rewriteJsonAST(json: JValue, camelize: Boolean): JValue = {
      json transform {
        case JField(nm, x) if !nm.startsWith("_") ⇒ JField(if (camelize) nm.camelize else nm.underscore, x)
        case x                                    ⇒ x
      }
    }
  }
}