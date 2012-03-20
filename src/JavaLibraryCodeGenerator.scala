package mojolly.swagger

import java.util.{List => JList}
import com.wordnik.swagger.codegen._
import resource.Resource
import org.antlr.stringtemplate._
import collection.JavaConversions._

abstract class JavaLibraryCodeGenerator(dir: String, conf: CodeGenConfig) extends LibraryCodeGenerator
    with mojolly.inflector.InflectorImports {
  {
    def pkg(name: String) = "%s.%s" format(conf.packageName, name)
    def srcDir = "%s/src/main/%s/%s" format (conf.libraryHome, dir, conf.packageName replace(".", "/"))
    initialize(conf.apiServerURL, conf.apiKey, pkg("model"), pkg("api"), srcDir, conf.libraryHome)
  }

  override protected def configureTemplateGroup(group: StringTemplateGroup) {
    group.registerRenderer(classOf[String], new ArgumentRenderer)
  }

    override protected def preprocess(resources: JList[Resource]) {
    super.preprocess(resources)
    for(resource <- resources) {
      for(model <- resource.getModels){
        for(modelField <- model.getFields) {
          modelField.setName(modelField.getName.camelize)
        }
      }
    }
  }
}
