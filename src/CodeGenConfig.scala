package mojolly.swagger

case class CodeGenConfig(apiServerURL: String,
                         apiKey: String,
                         packageName: String,
                         classOutputDir: String)

object CodeGenConfig {
  def apply(args: String*): CodeGenConfig = CodeGenConfig(args(0), args(1), args(2), args(3))
}
