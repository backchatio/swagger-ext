package mojolly.swagger

import java.io.File
import java.util.{List => JList}
import collection.JavaConversions._
import com.wordnik.swagger.codegen._
import config._
import common._
import js._
import resource._
import util.FileUtil
import org.antlr.stringtemplate._

object JavascriptLibCodeGen extends App {
  val codeGen = new JavascriptLibCodeGen(CodeGenConfig(args:_*))
  codeGen generateCode()

  class DataTypeMappingProvider extends JSDataTypeMappingProvider with DataTypeMappingProvider2 with mojolly.inflector.InflectorImports {
    override def getJsonIncludes: JList[String] = List()
    def getArgumentDefinition(method: ResourceMethod, arg: MethodArgument) = null // TODO
  }

  class NamingPolicyProvider extends CamelCaseNamingPolicyProvider

  val logger = org.slf4j.LoggerFactory.getLogger(classOf[JavascriptLibCodeGen])
}

class JavascriptLibCodeGen(config: CodeGenConfig) extends LibraryCodeGenerator {
  import JavascriptLibCodeGen._

  initialize(config.apiServerURL, config.apiKey, config.modelClassLocation.getAbsolutePath, config.resourceClassLocation.getAbsolutePath,
    config.classOutputDir.getAbsolutePath, config.libraryHome)

  setDataTypeMappingProvider(new DataTypeMappingProvider)
  setNameGenerator(new NamingPolicyProvider())
  EndpointOperation.setArgCountForInputModel(22)

  override def initializeLangConfig(langConfig: LanguageConfiguration) = {
    langConfig setClassFileExtension(".js")
    langConfig setTemplateLocation("conf/js/templates")
    langConfig setStructureLocation("conf/js/structure")
    langConfig setExceptionPackageName("com.wordnik.swagger.exception")
    langConfig setAnnotationPackageName("com.wordnik.swagger.annotations")

    FileUtil createOutputDirectories(langConfig.getResourceClassLocation(), langConfig.getClassFileExtension())
    FileUtil clearFolder(langConfig.getResourceClassLocation())
    FileUtil.copyDirectory(new File(langConfig.getStructureLocation()), config.resourceClassLocation)

    langConfig
  }
}
