package mojolly.swagger

import java.io.File
import java.util.{List => JList}
import collection.JavaConversions._
import com.wordnik.swagger.codegen._
import config._
import common._
import ruby._
import resource._
import util.FileUtil
import org.antlr.stringtemplate._

object RubyLibCodeGen extends App {
  val codeGen = new RubyLibCodeGen(CodeGenConfig(args:_*))
  codeGen generateCode()

  class DataTypeMappingProvider extends RubyDataTypeMappingProvider with DataTypeMappingProvider2 with mojolly.inflector.InflectorImports {
    override def getJsonIncludes: JList[String] = List()
    def getArgumentDefinition(method: ResourceMethod, arg: MethodArgument) = null // TODO
  }

  // camel case for classes, underscore for methods, properties, ...
  class NamingPolicyProvider extends CamelCaseNamingPolicyProvider with mojolly.inflector.InflectorImports {
    override def applyMethodNamingPolicy(name: String) = name.underscore
    override def getMethodName(endpoint: String, suggested: String) = applyMethodNamingPolicy(suggested)
  }

  class RubyCodeGenConfig(config: CodeGenConfig) {
    def resourceClassLocation = new File(classOutputDir, "api")
    def modelClassLocation = new File(classOutputDir, "model")
    def classOutputDir = new File(config.libraryHome, config.packageName)
  }

  implicit def config2ruby(config: CodeGenConfig) = new RubyCodeGenConfig(config)

  val logger = org.slf4j.LoggerFactory.getLogger(classOf[RubyLibCodeGen])
}

class RubyLibCodeGen(config: CodeGenConfig) extends LibraryCodeGenerator {
  import RubyLibCodeGen._

  initialize(config.apiServerURL, config.apiKey, config.modelClassLocation.getAbsolutePath, config.resourceClassLocation.getAbsolutePath,
    config.classOutputDir.getAbsolutePath, config.libraryHome)

  setDataTypeMappingProvider(new DataTypeMappingProvider)
  setNameGenerator(new NamingPolicyProvider())
  EndpointOperation.setArgCountForInputModel(22)

  override def initializeLangConfig(langConfig: LanguageConfiguration) = {
    langConfig setClassFileExtension(".rb")
    langConfig setTemplateLocation("conf/ruby/templates")
    langConfig setStructureLocation("conf/ruby/structure")
    langConfig setExceptionPackageName("com.wordnik.swagger.exception")
    langConfig setAnnotationPackageName("com.wordnik.swagger.annotations")

    FileUtil createOutputDirectories(langConfig.getResourceClassLocation(), langConfig.getClassFileExtension())
    FileUtil clearFolder(langConfig.getResourceClassLocation())
    FileUtil.copyDirectory(new File(langConfig.getStructureLocation()), config.resourceClassLocation)

    langConfig
  }
}
