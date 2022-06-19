package com.greenfossil.webserver

object AnnotatedPathMacroSupport extends MacroSupport(globalDebug = false) {

  import scala.quoted.*

  def computeActionAnnotatedPath[A <: EssentialAction : Type, R : Type](actionExpr: Expr[A],
                                                                        onSuccessCallback: (Expr[String], Expr[List[Any]], Expr[List[String]], Expr[List[Any]]) =>  Expr[R])
                                                                       (using Quotes): Expr[R] =
    import quotes.reflect.*
    searchForAnnotations(actionExpr.asTerm, 1) match
      case applyTerm @ Apply(_, paramValues) =>
        //Handle - Apply method
        show("ApplyTerm", applyTerm)
        val paramNames: List[String] = applyTerm.symbol.paramSymss.head.map(_.name)
        val paramNameValueLookup: Map[String, Term] = paramNames.zip(paramValues).toMap
        val annList = applyTerm.symbol.annotations
        getAnnotatedPath(actionExpr, annList, paramNameValueLookup, onSuccessCallback)

      case methodTerm if methodTerm.symbol.flags.is(Flags.Method) =>
        //Handle - Method
        val annList = methodTerm.symbol.annotations
        getAnnotatedPath(actionExpr, annList, Map.empty[String, Term], onSuccessCallback)

      case otherTerm =>
        show("otherTerm", otherTerm)
        val ref = Ref(otherTerm.symbol)
        val term = searchForAnnotations(ref, 1)
        show("search term", term)
        report.errorAndAbort("Unable to find any Essential Action path annotations")


  /**
   * Extract params from the annotated path
   * @param Quotes
   * @param actionExpr
   * @param annList
   * @param paramNameValueLookup
   * @param successCallback - on when the params/values length matched.
   *                        Query Params are those not found in the annotated path
   * @tparam A - Expr of an Annotated Essential Action or its subtye
   * @tparam P - Computed annotated path
   * @return
   */
  private def getAnnotatedPath[A <: EssentialAction : Type, P : Type](using Quotes)(
    actionExpr: Expr[A],
    annList: List[quotes.reflect.Term],
    paramNameValueLookup: Map[String, quotes.reflect.Term],
    successCallback: (Expr[String], Expr[List[Any]], Expr[List[String]], Expr[List[Any]]) =>  Expr[P]
  ): Expr[P] =
    import quotes.reflect.*
    getDeclaredPath(annList) match
      case None =>
        report.errorAndAbort(s"No annotated path found ${actionExpr}", actionExpr)

      case Some((method, declaredPath)) =>
        /*
         * update the del
         */
        val (computedPath, queryParamKeys, queryParamValues) = getComputedPathExpr(actionExpr, paramNameValueLookup, declaredPath)
        successCallback(Expr(method), Expr.ofList(computedPath), Expr[List[String]](queryParamKeys), Expr.ofList(queryParamValues))

  private def searchForAnnotations(using Quotes)(term: quotes.reflect.Term, level: Int): quotes.reflect.Term =
    import quotes.reflect.*
    //  show(s"SearchForAnnotations level: $level", term)
    term match
      case inlined @ Inlined(_, _, owner) =>
        show(s"Inlined...search level ${level}", inlined)
        searchForAnnotations(owner, level + 1)

      case foundTerm =>
        show("Term found", foundTerm)
        foundTerm

  /**
   *
   * @param Quotes
   * @param annList
   * @return Option[(GET|POST|PATH, path),
   */
  private def getDeclaredPath(using Quotes)(annList: List[quotes.reflect.Term]): Option[(String, String)] = {
    import quotes.reflect.*
    annList.collectFirst {
      case Apply(Select(New(annMethod), _), args) =>
        show("Annotation HttpMethod", annMethod)
        show("Annotation Path Parts", args)
        args.collectFirst { case Literal(c) => (annMethod.symbol.name, c.value.toString) }
    }.flatten
  }

  private def getComputedPathExpr[A <: EssentialAction : Type](using Quotes)(
    actionExpr: Expr[A],
    paramNameValueLookup: Map[String, quotes.reflect.Term],
    declaredPath: String): (List[Expr[Any]], List[String], List[Expr[Any]])  = {

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

    //compute Path
    val computedPath: List[Expr[Any]] =
      val parts = declaredPath.split("/:")
      parts.tail.foldLeft(List[Expr[Any]](Expr(parts.head))) { (accPath, part) =>
        val newParts = part.split("/").toList match
          case Nil =>  Nil
          case pathParamName +: rightParts => getPathParamExpr(pathParamName) +: rightParts.map(p => Expr(p))

        accPath ++ newParts
      }

    println(s"path parts ${computedPath.size}")
    computedPath foreach { part => showCode("part", part.asTerm, true) }

    //compute QueryString
    val queryParamKeys: List[String] = paramNameValueLookup.keys.toList diff usedPathParamNames
    println(s"queryParamKeys ${queryParamKeys.size}")
    queryParamKeys foreach println
    val queryParamValues: List[Expr[Any]] = queryParamKeys.map(k => paramNameValueLookup(k).asExprOf[Any])

    (computedPath, queryParamKeys, queryParamValues)
  }

}
