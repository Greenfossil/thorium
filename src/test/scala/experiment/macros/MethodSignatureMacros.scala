package experiment.macros

object MethodSignatureMacros {

  import com.greenfossil.webserver.*
  inline def MCall(inline action: Action): String =
    ${ MCallImpl('action) }

  import scala.quoted.*
  def MCallImpl(actionExpr:Expr[Action])(using Quotes): Expr[String] =
    import quotes.reflect.*
    
    val (name, annotationTerms, paramNameValueLookup) = actionExpr.asTerm match {
      case Inlined(_, _, methodOwner @ Select(_, name)) =>
        (name, methodOwner.symbol.annotations, Map.empty[String, Any])
      case Inlined(a, b, app @ Apply(Select(Ident(_),name ), args)) =>
        val paramNames: List[String] = app.symbol.paramSymss.head.map(_.name)
        val paramValues: List[Any] = args.collect{case Literal(c) => c.value}
        val paramNameValueLookup: Map[String, Any] = paramNames.zip(paramValues).toMap
        (name, app.symbol.annotations, paramNameValueLookup)
      case Inlined(_,_, Ident(name)) =>
        report.errorAndAbort("Action must be define as a function not value", actionExpr)
    }
//    println("name " + name)
//    println(s"annotations ${annotationTerms.size}")
//    annotationTerms foreach println
//    println(s"nvLookup ${paramNameValueLookup.size}")
//    paramNameValueLookup foreach println

    //(Method, Path) - assume there is a Path - TODO - need to harden this
    val (method, declaredPath): (String, String) = annotationTerms.collect{
      case Apply(Select(New(x), _), args) =>
        (x.symbol.name, args.collect{case Literal(c) => c}.head.value.toString)
    }.headOption.getOrElse((null, null))

    var usedPathParamNames: List[String] = Nil
    def getPathParam(name: String): Any =
      paramNameValueLookup.get(name) match {
        case Some(value) =>
          usedPathParamNames = usedPathParamNames :+ name
          value
        case None => report.errorAndAbort(s"Path param [${name}] does not match function param name", actionExpr)
      }

    val computedPath = 
      if paramNameValueLookup.isEmpty 
        then declaredPath
        else {
          val parts = declaredPath.split("/:")
          parts.tail.zipWithIndex.foldLeft(parts.head){(accPath, tup2) =>
            val (part, index) = tup2
            val newPart = part.split("/") match {
              case Array(pathParamName, right) =>
                s"${getPathParam(pathParamName)}/$right"
              case Array(pathParamName) =>
                getPathParam(pathParamName)
            }
            s"$accPath/$newPart"
          }
        }
    val mismatchParams =  paramNameValueLookup.keys.toList diff usedPathParamNames
    if mismatchParams.nonEmpty then report.errorAndAbort("Params mismatch", actionExpr)

    Expr(computedPath)


}
