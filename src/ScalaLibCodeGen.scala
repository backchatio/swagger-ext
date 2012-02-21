package mojolly.swagger

import com.wordnik.swagger.codegen._
import config.LanguageConfiguration
import config.scala.ScalaDataTypeMappingProvider
import util.FileUtil

import java.io.File

object ScalaLibCodeGen extends App {
  val codeGen = new ScalaLibCodeGen(CodeGenConfig(args:_*))
  codeGen generateCode()

  lazy val primitiveObjectMap = Map("string" -> "String",
    "String" -> "String",
    "java.lang.String" -> "String",
    "int" -> "Int",
    "integer" -> "Int",
    "Integer" -> "Int",
    "java.lang.Integer" -> "Int",
    "boolean" -> "Boolean",
    "Boolean" -> "Boolean",
    "java.lang.Boolean" -> "Boolean",
    "long" -> "Long",
    "Long" -> "Long",
    "java.lang.Long" -> "Long",
    "float" -> "Float",
    "Float" -> "Float",
    "java.lang.Float" -> "Float",
    "Date" -> "Date",
    "date" -> "Date",
    "byte" -> "Byte",
    "java.util.Date" -> "Date")

  class DataTypeMappingProvider extends ScalaDataTypeMappingProvider {
    import collection.JavaConversions._

    override def isPrimitiveType(input: String): Boolean = primitiveObjectMap.contains(input)

    override def getClassName(input: String, primitiveObject: Boolean): String = if (isPrimitiveType(input))
      if (primitiveObject) primitiveObjectMap(input) else ScalaDataTypeMappingProvider.primitiveValueMap(input)
    else nameGenerator applyClassNamingPolicy (input)

    override def getListIncludes(): java.util.List[String] = List.empty[String]
    override def getMapIncludes(): java.util.List[String] = List.empty[String]
    override def getSetIncludes: java.util.List[String] = List.empty[String]
  }
}

class ScalaLibCodeGen(config: CodeGenConfig) extends JavaLibraryCodeGenerator(config) {
  import ScalaLibCodeGen._

  setDataTypeMappingProvider(new DataTypeMappingProvider)

  override def initializeLangConfig(langConfig: LanguageConfiguration) = {
    langConfig setClassFileExtension(".scala")
    langConfig setTemplateLocation("conf/scala/templates")
    langConfig setStructureLocation("conf/scala/structure")
    langConfig setExceptionPackageName("com.wordnik.swagger.exception")
    langConfig setAnnotationPackageName("com.wordnik.swagger.annotations")

    //create ouput directories
    FileUtil createOutputDirectories(langConfig.getModelClassLocation(), langConfig.getClassFileExtension())
    FileUtil createOutputDirectories(langConfig.getResourceClassLocation(), langConfig.getClassFileExtension())
    FileUtil clearFolder(langConfig.getModelClassLocation())
    FileUtil clearFolder(langConfig.getResourceClassLocation())
    FileUtil.copyDirectory(new File(langConfig.getStructureLocation()), new File(config.libraryHome, "/src/main/scala/"));
    langConfig
  }
}