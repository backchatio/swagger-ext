package mojolly.swagger

import java.io.File
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

  class DataTypeMappingProvider extends PythonDataTypeMappingProvider

  class NamingPolicyProvider extends CamelCaseNamingPolicyProvider

  class PythonCodeGenConfig(config: CodeGenConfig) {
    def resourceClassLocation = new File(classOutputDir, "api")
    def modelClassLocation = new File(classOutputDir, "model")
    def classOutputDir = new File(config.libraryHome, config.packageName)
  }

  implicit def config2python(config: CodeGenConfig) = new PythonCodeGenConfig(config)
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

    FileUtil createOutputDirectories(langConfig.getModelClassLocation(), langConfig.getClassFileExtension())
    FileUtil createOutputDirectories(langConfig.getResourceClassLocation(), langConfig.getClassFileExtension())
    FileUtil clearFolder(langConfig.getModelClassLocation())
    FileUtil clearFolder(langConfig.getResourceClassLocation())
    FileUtil.copyDirectory(new File(langConfig.getStructureLocation()), config.resourceClassLocation)

    val initFile = new File(config.resourceClassLocation + "__init__.py")
    val newInitFile = new File(config.modelClassLocation + "__init__.py")
    initFile renameTo newInitFile

    langConfig
  }
}
