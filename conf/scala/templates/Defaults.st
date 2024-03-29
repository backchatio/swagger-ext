package $packageName$

import com.ning.http.client.AsyncHttpClient
import mojolly.swagger.runtime._

/**
 * NOTE: This class is auto generated by the swagger code generator program. Do not edit the class manually.
 *
 */
class DefaultClient extends ApiClient with $parents:{parent|$parent$};separator=" with "$ {
  val host = "$host$"
  val port = $port$
  val apiPath = "$path$"

  protected lazy val client = this
}

case class ApiKeyAuth(apiKey: String) extends ApiAuth {
   def headers = Map("Authorization" -> ("$token$ %s" format apiKey))
}
