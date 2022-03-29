package com.greenfossil.webserver


object AnnotatedPathMacroSupport {

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
        val sym = methodTerm.symbol

        val annList = methodTerm.symbol.annotations
        getAnnotatedPath(actionExpr, annList, Map.empty[String, Term], onSuccessCallback)

      case otherTerm =>
        val sym = otherTerm.symbol
        println(s">>>Other term flags ${sym.flags.show}")
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
        getComputedPathExpr(actionExpr, paramNameValueLookup, declaredPath) match {
          case (computedPathExpr, Nil) =>
            successCallback("POST", computedPathExpr)

          case (_, mismatchParams) =>
            report.errorAndAbort(s"Params missing [${mismatchParams.mkString(",")}]", actionExpr)
        }
    }

  def searchForAnnotations(using Quotes)(term: quotes.reflect.Term, level: Int): quotes.reflect.Term =
    import quotes.reflect.*
    //  show("SearchForAnnotations", term)
    if level == 1 then println("SearchForAnnotations...")
    term match {
      case inlined @ Inlined(_, _, owner) =>
        show(s"Inlined...search level ${level}", inlined)
        searchForAnnotations(owner, level + 1)

      case foundTerm =>
        show("Term found", foundTerm)
        foundTerm
    }

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
        val newParts = part.split("/") match
          case Array(pathParamName, right) => List(getPathParamExpr(pathParamName), Expr(right))
          case Array(pathParamName) => List(getPathParamExpr(pathParamName))

        accPath ++ newParts
      }

    val mismatchParams: Seq[String] = paramNameValueLookup.keys.toList diff usedPathParamNames
    val computedPathExpr: Expr[List[Any]] = Expr.ofList(computedPath)
    (computedPathExpr, mismatchParams)
  }

  def showStructure(using Quotes)(msg: String, x: quotes.reflect.Tree | List[quotes.reflect.Tree]): Unit =
    import quotes.reflect.*
    x match {
      case xs: List[Tree]  =>
        println(s"$msg: ${xs.map(_.show(using Printer.TreeStructure))}")

      case term: Tree =>
        println(s"$msg: ${term.show(using Printer.TreeStructure)}")

    }

  def showCode(using Quotes)(msg: String, x: quotes.reflect.Tree | List[quotes.reflect.Tree] ): Unit =
    import quotes.reflect.*
    x match {
      case xs: List[Tree]  =>
        println(s"$msg: ${xs.map(_.show(using quotes.reflect.Printer.TreeAnsiCode))}")

      case term: Tree =>
        println(s"$msg: ${term.show(using quotes.reflect.Printer.TreeAnsiCode)}")
    }

  def show(using Quotes)(msg: String, x: quotes.reflect.Tree | List[quotes.reflect.Tree]): Unit =
    import quotes.reflect.*
    x match {
      case xs: List[Tree]  =>
        println(s"===> [List] ${msg}")
        println(s"Code - Size:${xs.size}")
        println("  " + xs.map(_.show(using quotes.reflect.Printer.TreeAnsiCode)))

        println(s"Structure - Size:${xs.size}")
        println("  " + xs.map(_.show(using Printer.TreeStructure)))

      case term: Tree =>
        println(s"===> [Tree] ${msg}")
        println(s"Symbol: ${term.symbol.flags.show}")
        println(s"Code: ${term.show(using quotes.reflect.Printer.TreeAnsiCode)}")
        println(s"Struct: ${term.show(using Printer.TreeStructure)}")

    }


}
