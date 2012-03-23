package mojolly.swagger

import java.util.{List => JList}
import java.io.File
import collection.JavaConversions._
import com.wordnik.swagger.codegen._
import config.common.CamelCaseNamingPolicyProvider
import config.java.JavaDataTypeMappingProvider
import config.{DataTypeMappingProvider2, LanguageConfiguration}
import resource.{ModelField, Endpoint, EndpointOperation, Resource}
import util.FileUtil
import org.antlr.stringtemplate.StringTemplateGroup
import reflect.BeanProperty

object JavaLibCodeGen extends App with mojolly.inflector.InflectorImports  {
  val codeGen = new JavaLibCodeGen(CodeGenConfig(args: _*))
  codeGen generateCode()

  class DataTypeMappingProvider extends JavaDataTypeMappingProvider {
    override def getArgumentDefinition(t: String, n: String) = "%s %s" format (t, n.camelize)

  }

  class NamingPolicyProvider extends CamelCaseNamingPolicyProvider {
    override def createGetterMethodName(className: String, attributeName: String) = "%s.%s" format(className, applyMethodNamingPolicy(attributeName))
  }
}

class JavaLibCodeGen(config: CodeGenConfig) extends JavaLibraryCodeGenerator("java", config) {
  import JavaLibCodeGen._

  getConfig.setDefaultModelImports(List("mojolly.swagger.runtime.*"))
  setDataTypeMappingProvider(new DataTypeMappingProvider)
  setNameGenerator(new NamingPolicyProvider())
  EndpointOperation.setArgCountForInputModel(22)

  override def initializeLangConfig(langConfig: LanguageConfiguration) = {
    langConfig setClassFileExtension (".java")
    langConfig setTemplateLocation ("conf/java/templates")
    langConfig setStructureLocation ("conf/java/structure")
    langConfig setExceptionPackageName ("com.wordnik.swagger.exception")
    langConfig setAnnotationPackageName ("com.wordnik.swagger.annotations")

    FileUtil createOutputDirectories(langConfig.getModelClassLocation(), langConfig.getClassFileExtension())
    FileUtil createOutputDirectories(langConfig.getResourceClassLocation(), langConfig.getClassFileExtension())
    FileUtil clearFolder (langConfig.getModelClassLocation())
    FileUtil clearFolder (langConfig.getResourceClassLocation())
    FileUtil.copyDirectory(new File(langConfig.getStructureLocation()), new File(config.libraryHome, "/src/main/java/"))
    FileUtil.copyDirectory(new File("conf/jvm/structure"), new File(config.libraryHome, "/src/main/scala/"))

    langConfig
  }

  override def generateMiscClasses(resources: java.util.List[Resource], templateGroup: StringTemplateGroup) {
    generateParamClasses(resources, templateGroup)
  }

  def generateParamClasses(resources: java.util.List[Resource], templateGroup: StringTemplateGroup) {
    val ops = for (resource <- resources; ep <- resource.getEndPoints; op <- ep.getOperations) yield (resource, ep, op)
    ops foreach (op => generateParamClass(op._1, op._2, op._3, templateGroup))
  }

  def generateParamClass(res: Resource, ep: Endpoint, op: EndpointOperation, tp: StringTemplateGroup) {
    val className = "%sParams" format (nameGenerator applyClassNamingPolicy op.getNickname)
    val rm = op.generateMethod(ep, res, getDataTypeMappingProvider, nameGenerator)
    val imports = List("mojolly.swagger.runtime.*", "java.util.*", getConfig.getModelPackageName + ".*")
    val template = tp.getInstanceOf("Params")
    template.setAttribute("packageName", getConfig.getApiPackageName)
    template.setAttribute("imports", imports.toArray)
    template.setAttribute("className", className)
    template.setAttribute("params", Params(rm))
    template.setAttribute("method", rm)
    val f = new File(languageConfig.getResourceClassLocation + className + languageConfig.getClassFileExtension())
    writeFile(f, template.toString(), className)
  }

  class Params {
    @BeanProperty
    var requiredArgs: JList[String] = _

    @BeanProperty
    var hasRequiredArgs: Boolean = _

    @BeanProperty
    var args: JList[ModelField] = _

    @BeanProperty
    var headerArgs: JList[ModelField] = _

    @BeanProperty
    var pathArgs: JList[ModelField] = _

    @BeanProperty
    var queryArgs: JList[ModelField] = _
  }

  object Params {
    def apply(method: ResourceMethod) = {
      val params = new Params
      params.requiredArgs = method.getArguments filter (_.isRequired) map (m => "%s %s" format (m.getDataType, m.getName.camelize))
      params.hasRequiredArgs = params.requiredArgs.nonEmpty
      params.args = method.getArguments map (arg2Field(_))
      params.headerArgs = method.getHeaderParameters map (arg2Field(_))
      params.pathArgs = method.getPathParameters map (arg2Field(_))
      params.queryArgs = method.getQueryParameters map (arg2Field(_))
      params
    }

    def arg2Field(arg: MethodArgument) = {
      val f = new ModelField()
      f.setDescription(arg.getDescription)
      f.setName(arg.getName.camelize)
      f.setParamType(arg.getDataType)
      f.setRequired(arg.isRequired)
      f.getFieldDefinition(getDataTypeMappingProvider, getConfig, getNameGenerator)
      f
    }
  }
}