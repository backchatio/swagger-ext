package mojolly.swagger

import com.wordnik.swagger.codegen._
import config.LanguageConfiguration
import config.scala.ScalaDataTypeMappingProvider
import util.FileUtil

object ScalaLibCodeGen extends App {
  val codeGen = new ScalaLibCodeGen(CodeGenConfig(args:_*))
  codeGen generateCode()
}

class ScalaLibCodeGen(config: CodeGenConfig) extends JavaLibraryCodeGenerator(config) {
  setDataTypeMappingProvider(new ScalaDataTypeMappingProvider())

  override def initializeLangConfig(config: LanguageConfiguration) = {
    config setClassFileExtension(".scala")
    config setTemplateLocation("conf/scala/templates")
    config setStructureLocation("conf/scala/structure")
    config setExceptionPackageName("com.wordnik.swagger.exception")
    config setAnnotationPackageName("com.wordnik.swagger.annotations")

    //create ouput directories
    FileUtil createOutputDirectories(config.getModelClassLocation(), config.getClassFileExtension())
    FileUtil createOutputDirectories(config.getResourceClassLocation(), config.getClassFileExtension())
    FileUtil clearFolder(config.getModelClassLocation())
    FileUtil clearFolder(config.getResourceClassLocation())
    config
  }
}