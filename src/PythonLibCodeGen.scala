package mojolly.swagger

import java.io.File
import java.util.{List => JList}
import collection.JavaConversions._
import com.wordnik.swagger.codegen._
import config._
import common._
import python._
import resource._
import util.FileUtil
import org.antlr.stringtemplate._

object PythonLibCodeGen extends App {
  val codeGen = new PythonLibCodeGen(CodeGenConfig(args:_*))
  codeGen generateCode()

  class DataTypeMappingProvider extends PythonDataTypeMappingProvider with DataTypeMappingProvider2 with mojolly.inflector.InflectorImports {
    override def getJsonIncludes: JList[String] = List()

    def getArgumentDefinition(method: ResourceMethod, arg: MethodArgument) = {
      val default = arg.getDataType match {
        case "bool" => Option(arg.getDefaultValue) map (_.capitalize) getOrElse "False"
        case x if x.startsWith("list") => "[]"
        case _ => "None"
      }
      val fmt = if (!arg.isRequired) "%s = " + (default) else "%s"
      fmt format arg.getName.underscore
    }
  }

  class NamingPolicyProvider extends CamelCaseNamingPolicyProvider with mojolly.inflector.InflectorImports {
    override def applyMethodNamingPolicy(name: String) = name.underscore
    override def getMethodName(endpoint: String, suggested: String) = applyMethodNamingPolicy(suggested)
  }

  class PythonCodeGenConfig(config: CodeGenConfig) {
    def resourceClassLocation = new File(classOutputDir, "api")
    def modelClassLocation = new File(classOutputDir, "model")
    def classOutputDir = new File(config.libraryHome, config.packageName)
  }

  implicit def config2python(config: CodeGenConfig) = new PythonCodeGenConfig(config)

  val logger = org.slf4j.LoggerFactory.getLogger(classOf[PythonLibCodeGen])
}

class PythonLibCodeGen(config: CodeGenConfig) extends LibraryCodeGenerator {
  import PythonLibCodeGen._

  initialize(config.apiServerURL, config.apiKey, config.modelClassLocation.getAbsolutePath, config.resourceClassLocation.getAbsolutePath,
    config.classOutputDir.getAbsolutePath, config.libraryHome)

  setDataTypeMappingProvider(new DataTypeMappingProvider)
  setNameGenerator(new NamingPolicyProvider())
  EndpointOperation.setArgCountForInputModel(22)

  override def initializeLangConfig(langConfig: LanguageConfiguration) = {
    langConfig setClassFileExtension(".py")
    langConfig setTemplateLocation("conf/python/templates")
    langConfig setStructureLocation("conf/python/structure")
    langConfig setExceptionPackageName("com.wordnik.swagger.exception")
    langConfig setAnnotationPackageName("com.wordnik.swagger.annotations")

    FileUtil createOutputDirectories(langConfig.getResourceClassLocation(), langConfig.getClassFileExtension())
    FileUtil clearFolder(langConfig.getResourceClassLocation())
    FileUtil.copyDirectory(new File(langConfig.getStructureLocation()), config.resourceClassLocation)

    touchInitFiles(langConfig)

    langConfig
  }

  override def generateModelClasses(resources: JList[Resource], templateGroup: StringTemplateGroup) {
    val models = new collection.mutable.ListBuffer[Model]()
    for (resource <- (resources)) {
      for (model <- resource.getModels) {
        if (!models.exists(_.getName == model.getName) && !this.getCodeGenRulesProvider.isModelIgnored(model.getName)) {
          if (null == model.getFields || model.getFields.size == 0)
            logger.warn("Model " + model.getName + " doesn't have any properties")
          else
            models += model
        }
      }
    }

    var template: StringTemplate = templateGroup.getInstanceOf("Init")
    template.setAttribute("models", models.toArray)
    var aFile: File = new File(languageConfig.getResourceClassLocation, "__init__" + languageConfig.getClassFileExtension)
    writeFile(aFile, template.toString, "__init__")
  }

  def touchInit(f: File) = new File(f,  "__init__.py").createNewFile

  def touchInitFiles(langConfig: LanguageConfiguration) = {
    var f: File = new File(config.libraryHome)
    for (dir <- config.packageName.split("/")) {
      f = new File(f, dir)
      touchInit(f)
    }
  }
}
