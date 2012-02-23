package mojolly.swagger.runtime

trait ApiError

class JsonParseError() extends ApiError
class IoError() extends ApiError