package mojolly.swagger

import java.util.{List => JList}
import java.io.File
import com.wordnik.swagger.codegen._
import config.common.CamelCaseNamingPolicyProvider
import config.scala.ScalaDataTypeMappingProvider
import config.{DataTypeMappingProvider2, LanguageConfiguration}
import resource.{EndpointOperation, Resource}
import util.FileUtil
import org.antlr.stringtemplate.StringTemplateGroup

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
    "Date" -> "DateTime",
    "date" -> "DateTime",
    "byte" -> "Byte",
    "java.util.Date" -> "DateTime",
    "json" -> "JValue")

  class DataTypeMappingProvider extends ScalaDataTypeMappingProvider with DataTypeMappingProvider2 {
    import collection.JavaConversions._

    override def isPrimitiveType(input: String): Boolean = primitiveObjectMap.contains(input)

    override def getClassName(input: String, primitiveObject: Boolean): String = if (isPrimitiveType(input))
      if (primitiveObject) primitiveObjectMap(input) else ScalaDataTypeMappingProvider.primitiveValueMap(input)
    else nameGenerator applyClassNamingPolicy (input)

    override def getListIncludes(): JList[String] = List.empty[String]
    override def getMapIncludes(): JList[String] = List.empty[String]
    override def getSetIncludes: JList[String] = List.empty[String]
    override def getDateIncludes: java.util.List[String] = List("org.joda.time.DateTime")
    override def getJsonIncludes: java.util.List[String] = List("net.liftweb.json.JValue")

    def getArgumentDefinition(method: ResourceMethod, arg: MethodArgument) = (if (method.getInputModel != null || arg.isRequired) "%s: %s" else "%s: Option[%s] = None") format  (arg.getName, arg.getDataType)
  }

  class NamingPolicyProvider extends CamelCaseNamingPolicyProvider {
    override def createGetterMethodName(className: String, attributeName: String) = "%s.%s" format (className, applyMethodNamingPolicy(attributeName))
  }
}

class ScalaLibCodeGen(config: CodeGenConfig) extends JavaLibraryCodeGenerator(config) {
  import ScalaLibCodeGen._

  setDataTypeMappingProvider(new DataTypeMappingProvider)
  setNameGenerator(new NamingPolicyProvider())
  EndpointOperation.setArgCountForInputModel(22)

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

  override def generateMiscClasses(resources: java.util.List[Resource], templateGroup: StringTemplateGroup) {
    val template = templateGroup.getInstanceOf("Defaults")
    template.setAttribute("host", config.apiHostConfig.host)
    template.setAttribute("port", config.apiHostConfig.port)
    template.setAttribute("path", config.apiHostConfig.path)
    template.setAttribute("token", config.apiHostConfig.authToken)
    template.setAttribute("packageName", getConfig.getApiPackageName);
    val f = new File(languageConfig.getResourceClassLocation + "Defaults" + languageConfig.getClassFileExtension())
    writeFile(f, template.toString(), "Defaults");
  }
}