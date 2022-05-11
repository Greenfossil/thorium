package com.greenfossil.webserver


object AnnotatedPathMacroSupport extends MacroSupport(debug =false) {

  import com.linecorp.armeria.server.annotation.AnnotatedHttpService

  import scala.quoted.*

  def computeActionAnnotatedPath[A <: AnnotatedHttpService : Type, R : Type](actionExpr: Expr[A],
                                                                             onSuccessCallback: (String, Expr[List[Any]]) =>  Expr[R]
                                                                            )(using Quotes): Expr[R] =
    import quotes.reflect.*
    searchForAnnotations(actionExpr.asTerm, 1) match {

      case applyTerm @ Apply(_, paramValues) =>
        show("ApplyTerm", applyTerm)
        val paramNames: List[String] = applyTerm.symbol.paramSymss.head.map(_.name)
        val paramNameValueLookup: Map[String, Term] = paramNames.zip(paramValues).toMap
        val annList = applyTerm.symbol.annotations
        getAnnotatedPath(actionExpr, annList, paramNameValueLookup, onSuccessCallback)

      case methodTerm if methodTerm.symbol.flags.is(Flags.Method) =>
        val annList = methodTerm.symbol.annotations
        getAnnotatedPath(actionExpr, annList, Map.empty[String, Term], onSuccessCallback)

      case otherTerm =>
        report.errorAndAbort("Unable to find annotations")
    }

  def getAnnotatedPath[A <: AnnotatedHttpService : Type, R : Type](using Quotes)(
    actionExpr: Expr[A],
    annList: List[quotes.reflect.Term],
    paramNameValueLookup: Map[String, quotes.reflect.Term],
    successCallback: (String, Expr[List[Any]]) =>  Expr[R]
  ): Expr[R] =
    import quotes.reflect.*
    getDeclaredPath(annList) match {
      case None =>
        report.errorAndAbort(s"No annotated path found ${actionExpr}", actionExpr)

      case Some(declaredPath) if paramNameValueLookup.isEmpty =>
        //Constant Path
        successCallback("POST", Expr(List(declaredPath)))

      case Some(declaredPath) =>
        getComputedPathExpr(actionExpr, paramNameValueLookup, declaredPath) match
          case (computedPathExpr, Nil) =>
            successCallback("POST", computedPathExpr)

          case (_, mismatchParams) =>
            report.errorAndAbort(s"Annotated endpoint has params missing [${mismatchParams.mkString(",")}]", actionExpr)
    }

  def searchForAnnotations(using Quotes)(term: quotes.reflect.Term, level: Int): quotes.reflect.Term =
    import quotes.reflect.*
    //  show(s"SearchForAnnotations level: $level", term)
    term match
      case inlined @ Inlined(_, _, owner) =>
        show(s"Inlined...search level ${level}", inlined)
        searchForAnnotations(owner, level + 1)

      case foundTerm =>
        show("Term found", foundTerm)
        foundTerm

  def getDeclaredPath(using Quotes)(annList: List[quotes.reflect.Term]): Option[String] = {
    import quotes.reflect.*
    annList.collectFirst {
      case Apply(Select(New(annMethod), _), args) =>
        show("Annotation HttpMethod", annMethod)
        show("Annotation Path Parts", args)
        args.collectFirst { case Literal(c) => c.value.toString }
    }.flatten
  }

  def getComputedPathExpr[A <: AnnotatedHttpService : Type](using Quotes)(
    actionExpr: Expr[A],
    paramNameValueLookup: Map[String, quotes.reflect.Term],
    declaredPath: String): (Expr[List[Any]], Seq[String])  = {

    import quotes.reflect.*

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
        val newParts = part.split("/").toList match
          case pathParamName +: rightParts => getPathParamExpr(pathParamName) +: rightParts.map(p => Expr(p))

        accPath ++ newParts
      }

    val mismatchParams: Seq[String] = paramNameValueLookup.keys.toList diff usedPathParamNames
    val computedPathExpr: Expr[List[Any]] = Expr.ofList(computedPath)
    (computedPathExpr, mismatchParams)
  }

}
