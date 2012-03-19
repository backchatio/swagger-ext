package mojolly.swagger

import java.util.{List => JList}
import java.io.File
import collection.JavaConversions._
import com.wordnik.swagger.codegen._
import config.common.CamelCaseNamingPolicyProvider
import config.java.JavaDataTypeMappingProvider
import config.{DataTypeMappingProvider2, LanguageConfiguration}
import resource.{EndpointOperation, Resource}
import util.FileUtil
import org.antlr.stringtemplate.StringTemplateGroup

object JavaLibCodeGen extends App {
  val codeGen = new JavaLibCodeGen(CodeGenConfig(args: _*))
  codeGen generateCode()

  class DataTypeMappingProvider extends JavaDataTypeMappingProvider

  class NamingPolicyProvider extends CamelCaseNamingPolicyProvider {
    override def createGetterMethodName(className: String, attributeName: String) = "%s.%s" format(className, applyMethodNamingPolicy(attributeName))
  }

}

class JavaLibCodeGen(config: CodeGenConfig) extends JavaLibraryCodeGenerator("java", config) {
  import JavaLibCodeGen._

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

    langConfig
  }

  override def generateMiscClasses(resources: java.util.List[Resource], templateGroup: StringTemplateGroup) {
    generateParamClasses(resources, templateGroup)
  }

  def generateParamClasses(resources: java.util.List[Resource], templateGroup: StringTemplateGroup) {
    val ops = for (resource <- resources; ep <- resource.getEndPoints; m <- ep.getOperations) yield m
    ops foreach (op => generateParamClass(op, templateGroup))
  }

  def generateParamClass(method: EndpointOperation, templateGroup: StringTemplateGroup) {
    val className = "%sParams" format (nameGenerator applyClassNamingPolicy method.getNickname)
    val imports = List("mojolly.swagger.runtime.*")
    val template = templateGroup.getInstanceOf("Params")
    template.setAttribute("packageName", getConfig.getApiPackageName)
    template.setAttribute("imports", imports.toArray)
    template.setAttribute("className", className)
    val f = new File(languageConfig.getResourceClassLocation + className + languageConfig.getClassFileExtension())
    writeFile(f, template.toString(), className)
  }
}