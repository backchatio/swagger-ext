package $packageName$;

$imports:{ import |
import $import$;
}$

public class $className$ extends Params {

$params.args:{ arg |
  /**
   * $arg.description$
   */
  private $arg.fieldDefinition.returnType$ $arg.fieldDefinition.name;format="camelcase"$ $arg.fieldDefinition.initialization$;$\r$};separator="\n"$

  public $className$() {
  }
$if(params.hasRequiredArgs)$

  public $className$($params.requiredArgs;separator=", "$) {
  }
$endif$

$params.args:{ arg |

  /**
    * $arg.description$
    */
  public $arg.fieldDefinition.returnType$ get$arg.fieldDefinition.NameForMethod$() {
    return $arg.fieldDefinition.name$;
  }

  public void set$arg.fieldDefinition.NameForMethod$($arg.fieldDefinition.returnType$ $arg.fieldDefinition.name$) {
    this.$arg.fieldDefinition.name$ = $arg.fieldDefinition.name$;
  }
};separator="\n"$

  public List<Param> getHeaderParams() {
    List<Param> params = new ArrayList();
    $params.headerArgs:{arg|params.addAll(createParams("$arg.fieldDefinition.name;format="underscore"$", $arg.fieldDefinition.name$));};separator="\n"$
    return params;
  }

  public List<Param> getQueryParams() {
    List<Param> params = new ArrayList();
    $params.queryArgs:{arg|params.addAll(createParams("$arg.fieldDefinition.name;format="underscore"$", $arg.fieldDefinition.name$));};separator="\n"$
    return params;
  }

  public List<Param> getPathParams() {
    List<Param> params = new ArrayList();
    $params.pathArgs:{arg|params.addAll(createParams("$arg.fieldDefinition.name;format="underscore"$", $arg.fieldDefinition.name$));};separator="\n"$
    return params;
  }
}