package mojolly.swagger

import java.io.File

case class CodeGenConfig(apiServerURL: String,
                         apiKey: String,
                         packageName: String,
                         libraryHome: String,
                         apiHostConfig: ApiHostConfig) {

  def resourceClassLocation = new File(classOutputDir, "api")
  def modelClassLocation = new File(classOutputDir, "model")
  def classOutputDir = new File(libraryHome, packageName)
}

case class ApiHostConfig(authToken: String, host: String, port: Int = 80, path: String = "")

object CodeGenConfig {
  def apply(args: String*): CodeGenConfig = CodeGenConfig(args(0), args(1), args(2), args(3),
    ApiHostConfig(args(7), args(4), parsePort(args(5)), args(6)))
  def parsePort(p: String) = try { Integer.parseInt(p) } catch { case _ => 80 }
}
