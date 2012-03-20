package mojolly.swagger

import org.antlr.stringtemplate._

class ArgumentRenderer extends AttributeRenderer with mojolly.inflector.InflectorImports {
  def toString(o: AnyRef) = o.toString
  def toString(o: AnyRef, formatName: String) = (toString(o) /: formatName.split(",")) { case (r, f) => format(r, f.trim())}
  def format(o: String, formatName: String) = formatName match {
    case "underscore" => o.underscore
    case "camelcase" => o.camelize
    case "capital" => o.capitalize
    case _ => o
  }
}
