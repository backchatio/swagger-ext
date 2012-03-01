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

trait ApiResource {
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

  def submit[T](method: String, path: String,
                queryParams: Iterable[(String, String)],
                headers: Map[String, String], authRequired: Boolean)(implicit auth: ApiAuth, mf: scala.reflect.Manifest[T]): Either[ApiError, T] = {
    val u = URI.create(path)
    val reqUri = if (u.isAbsolute) u else new URI("http", null, host, port, apiPath + u.getPath, u.getQuery, u.getFragment)
    val req = (requestFactory(method)
      andThen (addHeaders(headers) _)
      andThen (addAuth(auth, authRequired) _)
      andThen (addParameters(method, queryParams) _))(reqUri.toASCIIString)
    parseResponse(req.execute(async).get)
  }

  def parseResponse[T](resp: ClientResponse)(implicit mf: scala.reflect.Manifest[T]): Either[ApiError, T] = withResponse(resp) { json =>
    json.extract[T]
  }

  def withResponse[T](response: ClientResponse)(f: JValue => T)(implicit mf: scala.reflect.Manifest[T]): Either[ApiError, T] = {
    (parseOpt(response.body) toRight (new JsonParseError())).right flatMap {
      case JObject(JField("data", data) :: JField("errors", JArray(errors)) :: Nil) =>
        if (errors.isEmpty)
          (catching(classOf[IOException])).either { f(data) }.left map (_ => new IoError)
        else
          Left(new ApiError {})
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
    if (required) auth.populateSecurityInfo(req)
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

  abstract class ApiOperation[T](val method: String, val pathPattern: String) {
    def queryParams: Iterable[(String, String)]
    def headerParams: Map[String, String]
    def pathParams: Map[String, String]
    def path = pathPattern
  }

  object Params {
    trait Param {
      def key: String
      def value: List[String]
    }
    case class CParam(key: String, value: List[String]) extends Param
    object Param {
      implicit def any2param[T](p: (String, T))(implicit mf: Manifest[T]): Param = {
        val opt = if (p._2 != null && mf.erasure == classOf[Option[_]]) p._2.asInstanceOf[Option[_]] else Option(p._2)
        CParam(p._1, (opt map (_.toString)).toList)
      }
      implicit def list2param(p: (String, List[Any])): Param =
        CParam(p._1, Option(p._2) map (l => l map (_.toString)) getOrElse Nil)
    }
    def apply(params: Param*): Iterable[(String,  String)] = (params filterNot (p => p.key == null || p.value.isEmpty) flatMap (p => p.value map (s => (p.key, s))))
  }

  implicit def apiOperation2result[T](op: ApiOperation[T])(implicit auth: ApiAuth, mf: scala.reflect.Manifest[T]): Either[ApiError, T] = {
    val path = (op.path /: op.pathParams) { (path, param) => path.replace(("{%s}" format param._1), param._2) }
    submit(op.method, path, op.queryParams, op.headerParams, true)
  }
}