package com.greenfossil.webserver

/**
 * Inline redirect macro
 * @param action
 * @return
 */
inline def RedirectMcr(inline action: Action): Result =
  ${ RedirectMcrImpl('action) }

import scala.quoted.*
def RedirectMcrImpl(actionExpr:Expr[Action])(using Quotes): Expr[Result] = {
  import quotes.reflect.*

  val (methodOwner, paramNameValueLookup) = actionExpr.asTerm match {

    /*  Match using double Inlined
     * action with arglist
     */
    case Inlined(_, _, Inlined(_, _, methodOwner@Apply(_, paramValues))) =>
      val paramNames: List[String] = methodOwner.symbol.paramSymss.head.map(_.name)
      val paramNameValueLookup: Map[String, Term] = paramNames.zip(paramValues).toMap
      (methodOwner, paramNameValueLookup)

    /* Match using double Inlined
     * action with no arglist
     */
    case Inlined(_, _, Inlined(_, _, methodOwner)) =>
      (methodOwner, Map.empty[String, Term])

  }

  def getDeclaredPath(methodOwner: Term): Option[String] = {
    println(s"MethodOwner ${methodOwner.show(using Printer.TreeAnsiCode)}")
    val annList = methodOwner.symbol.annotations
    println(s"--> ann = ${annList}")
    annList.collectFirst {
      case Apply(Select(New(annMethod), _), args) =>
        println(s"Annotation Method ${annMethod.show(using Printer.TreeAnsiCode)}")
        args.foreach(arg => println(s"Path ${arg.show(using Printer.TreeAnsiCode)}"))
        args.collectFirst { case Literal(c) => c.value.toString }
    }.flatten
  }

  def getComputedRedirect(paramNameValueLookup: Map[String, Term], declaredPath: String): Expr[Result] = {
    //Parameterized Path
    var usedPathParamNames: List[String] = Nil

    def getPathParamExpr(name: String): Expr[Any] = {
      paramNameValueLookup.get(name) match
        case Some(value) =>
          usedPathParamNames = usedPathParamNames :+ name
          value.asExpr
        case None =>
          report.errorAndAbort(s"Path param [$name] of path [$declaredPath] cannot be found in method's param names [${paramNameValueLookup.keySet.mkString(",")}]  ", actionExpr)
    }

    val computedPath: List[Expr[Any]] =
      val parts = declaredPath.split("/:")
      parts.tail.foldLeft(List[Expr[Any]](Expr(parts.head))) { (accPath, part) =>
        val newParts = part.split("/") match
          case Array(pathParamName, right) => List(getPathParamExpr(pathParamName), Expr(right))
          case Array(pathParamName) => List(getPathParamExpr(pathParamName))

        accPath ++ newParts
      }

    paramNameValueLookup.keys.toList diff usedPathParamNames match
      case Nil =>
        val computedPathExpr: Expr[List[Any]] = Expr.ofList(computedPath)
        '{Redirect($ {computedPathExpr}.mkString("/"))}

      case mismatchParams =>
        report.errorAndAbort(s"Params missing [${mismatchParams.mkString(",")}]", actionExpr)
  }

  getDeclaredPath(methodOwner) match {
    case None =>
      report.errorAndAbort("No annotated path found", actionExpr)

    case Some(declaredPath) if paramNameValueLookup.isEmpty =>
      //Constant Path
      '{Redirect($ {Expr(declaredPath)})}

    case Some(declaredPath) =>
      getComputedRedirect(paramNameValueLookup, declaredPath)
  }

}