package mojolly.swagger.runtime

trait ApiResource {
  def submit[T](method: String, path: String): Either[ApiError, T] = Left(null)
}