package mojolly.swagger

import org.antlr.stringtemplate._

class ArgumentRenderer extends AttributeRenderer with mojolly.inflector.InflectorImports {
  def toString(o: AnyRef) = o.toString
  def toString(o: AnyRef, formatName: String) = formatName match {
    case "underscore" => toString(o).underscore
    case "camelcase" => toString(o).camelize
    case _ => toString(o)
  }
}
