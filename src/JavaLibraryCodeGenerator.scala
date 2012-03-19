package mojolly.swagger

import com.wordnik.swagger.codegen._
import org.antlr.stringtemplate._

abstract class JavaLibraryCodeGenerator(dir: String, config: CodeGenConfig) extends LibraryCodeGenerator {
  {
    def pkg(name: String) = "%s.%s" format(config.packageName, name)
    def srcDir = "%s/src/main/%s/%s" format (config.libraryHome, dir, config.packageName replace(".", "/"))
    initialize(config.apiServerURL, config.apiKey, pkg("model"), pkg("api"), srcDir, config.libraryHome)
  }

  override protected def configureTemplateGroup(group: StringTemplateGroup) {
    group.registerRenderer(classOf[String], new ArgumentRenderer)
  }
}
