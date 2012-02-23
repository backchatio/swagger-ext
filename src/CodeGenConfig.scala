package mojolly.swagger

case class CodeGenConfig(apiServerURL: String,
                         apiKey: String,
                         packageName: String,
                         libraryHome: String,
                         apiHostConfig: ApiHostConfig)

case class ApiHostConfig(host: String, port: Int = 80, path: String = "")

object CodeGenConfig {
  def apply(args: String*): CodeGenConfig = CodeGenConfig(args(0), args(1), args(2), args(3),
    ApiHostConfig(args(4), parsePort(args(5)), args(6)))
  def parsePort(p: String) = try { Integer.parseInt(p) } catch { case _ => 80 }
}
