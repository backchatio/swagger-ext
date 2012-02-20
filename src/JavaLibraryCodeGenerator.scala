package mojolly.swagger

import com.wordnik.swagger.codegen._
import config.common.CamelCaseNamingPolicyProvider

abstract class JavaLibraryCodeGenerator(config: CodeGenConfig) extends LibraryCodeGenerator {
  {
    def pkg(name: String) = "%s.%s" format(config.packageName, name)
    initialize(config.apiServerURL, config.apiKey, pkg("model"), pkg("api"), config.classOutputDir, "")
    setNameGenerator(new CamelCaseNamingPolicyProvider())
  }
}
