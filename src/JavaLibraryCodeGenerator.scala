package mojolly.swagger

import com.wordnik.swagger.codegen._
import org.antlr.stringtemplate._

abstract class JavaLibraryCodeGenerator(config: CodeGenConfig) extends LibraryCodeGenerator {
  {
    def pkg(name: String) = "%s.%s" format(config.packageName, name)
    initialize(config.apiServerURL, config.apiKey, pkg("model"), pkg("api"), config.libraryHome + "/src/main/scala/" + (config.packageName replace(".", "/")), config.libraryHome)
  }

  override protected def configureTemplateGroup(group: StringTemplateGroup) {
    group.registerRenderer(classOf[String], new ArgumentRenderer)
  }
}
