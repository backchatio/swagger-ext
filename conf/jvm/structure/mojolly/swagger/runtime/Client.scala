package mojolly.swagger.runtime

import java.net.URI
import java.util.Locale.ENGLISH
import java.nio.charset.Charset
import java.io.IOException
import java.util.{List => JList}

import scalax.io.{Codec => Codecx, Resource}
import io.Codec
import collection.JavaConversions._
import reflect.BeanProperty

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

class Param(@BeanProperty var key: String, @BeanProperty var value: String)

class ClientConfig {
  @BeanProperty
  var host: String = _
  @BeanProperty
  var port: Int = _
  @BeanProperty
  var apiPath: String = _
  @BeanProperty
  var headers: Map[String, String] = _
}

class JvmClient(config: ClientConfig) extends ApiClient {
  val host = config.host
  val port = config.port
  val apiPath = config.apiPath

  def submit[T](method: String, path: String,
                queryParams: JList[Param],
                pathParams: JList[Param],
                headers: JList[Param], authRequired: Boolean, cls: Class[T]): T = {
    val p = (path /: pathParams) { (path, param) => path.replace(("{%s}" format param.key), param.value) }
    val h = (Map.empty[String,  String] /: headers) { (m, param) => m + (param.key -> param.value) }
    submit(method, p, queryParams map (p => (p.key, p.value)), h, authRequired)(null, Manifest.classType(cls)) match {
      case Left(e) => throw new Exception()
      case Right(m) => m
    }
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

class Tuple {

}

class Json {

}