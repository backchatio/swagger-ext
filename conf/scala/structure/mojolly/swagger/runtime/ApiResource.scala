package mojolly.swagger.runtime

import java.net.URI
import java.util.Locale.ENGLISH
import java.nio.charset.Charset

import scalax.io.{Codec => Codecx, Resource}
import io.Codec
import collection.JavaConversions._

import com.ning.http.client._

trait ApiResource {
  private val clientConfig = new AsyncHttpClientConfig.Builder().setFollowRedirects(false).build()
  private val underlying = new AsyncHttpClient(clientConfig)

  def host: String
  def port: Int

  def close() {
    underlying.close()
  }

  def submit[T](method: String, path: String,
                queryParams: Map[String, String],
                headers: Map[String, String]): Either[ApiError, T] = {
    val u = URI.create(path)
    val reqUri = if (u.isAbsolute) u else new URI("http", null, host, port, u.getPath, u.getQuery, u.getFragment)
    val req = (requestFactory(method)
      andThen (addHeaders(headers) _)
      andThen (addParameters(method, queryParams) _))(reqUri.toASCIIString)
    val res = req.execute(async).get
    println(res.body)
    null
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
    def queryParams: Map[String, String]
    def headerParams: Map[String, String]
    def path = pathPattern
  }

  object Params {
    def apply(params: (String, String)*): Map[String,  String] = (params filterNot (_ == null)).toMap
  }

  implicit def apiOperation2result[T](op: ApiOperation[T]): Either[ApiError, T] =
    submit(op.method, op.path, op.queryParams, op.headerParams)
}