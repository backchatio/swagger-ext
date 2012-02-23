package mojolly.swagger.runtime

import com.ning.http.client.AsyncHttpClient

trait ApiAuth {
  def populateSecurityInfo(req: AsyncHttpClient#BoundRequestBuilder)
}