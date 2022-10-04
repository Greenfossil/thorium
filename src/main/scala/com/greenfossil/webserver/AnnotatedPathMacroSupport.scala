package com.greenfossil.webserver

import com.linecorp.armeria.internal.shaded.guava.net.UrlEscapers

import java.nio.charset.{Charset, StandardCharsets}

object AnnotatedPathMacroSupport extends MacroSupport(globalDebug = false) {

  def urlEncode(str: String): String =
    java.net.URLEncoder.encode(str, StandardCharsets.UTF_8).replaceAll("\\+", "%20")

  import scala.quoted.*

  def computeActionAnnotatedPath[A : Type, R : Type](epExpr: Expr[A],
                                                     onSuccessCallback: (Expr[String], Expr[List[Any]], Expr[List[String]], Expr[List[Any]]) =>  Expr[R],
                                                     supportQueryStringPost: Boolean = true
                                                    )(using Quotes): Expr[R] =
    import quotes.reflect.*
    searchForAnnotations(epExpr.asTerm, 1) match
      case applyTerm: Apply =>
        show("Apply Term", applyTerm)
        val paramSymss: List[List[Symbol]] = applyTerm.symbol.paramSymss

        //Get all param names annotated with @Param from all paramList
        import com.linecorp.armeria.server.annotation.Param
        val annotatedParamNames: List[String] = paramSymss.flatten.collect{
          case sym: Symbol if sym.annotations.exists(_.symbol.fullName.startsWith(classOf[Param].getName)) =>
            sym.name
        }

        //Get all param names from all paramList
        val paramNames: List[String] = paramSymss.flatten.map(_.name)

        //Removed the params that are not annotated with @Param
        val paramValues = getFlattenedParamValues(applyTerm)
        val paramNameValueLookup: Map[String, Term] =
          paramNames.zip(paramValues)
            .toMap
            .filter((key, value) => annotatedParamNames.contains(key))

        val annList = applyTerm.symbol.annotations //Get Endpoint annotations
        getAnnotatedPath(epExpr, annList, paramNameValueLookup, onSuccessCallback, supportQueryStringPost)

      case typedTerm: Typed =>
        report.errorAndAbort(s"Check function body for '???' code", epExpr)

      case methodTerm if methodTerm.symbol.flags.is(Flags.Method) =>
        //Handle - Method
        show("Method Term", methodTerm)
        val annList = methodTerm.symbol.annotations
        getAnnotatedPath(epExpr, annList, Map.empty[String, Term], onSuccessCallback, supportQueryStringPost)

      case otherTerm =>
        show("otherTerm", otherTerm)
        val ref = Ref(otherTerm.symbol)
        val term = searchForAnnotations(ref, 1)
        show("search term", term)
        report.errorAndAbort("Unable to find any annotated path")

  /**
   * Get a flatten list of param values from the a list of list of param values
   * @param Quotes
   * @param applyTerm
   * @return
   */
  private def getFlattenedParamValues(using Quotes)(applyTerm: quotes.reflect.Apply): List[quotes.reflect.Term] =
    import quotes.reflect.*
    applyTerm match
      case Apply(aTerm: Apply, paramValues) => getFlattenedParamValues(aTerm) ++ paramValues
      case Apply(_, paramValues) => paramValues

  /**
   * Extract params from the annotated path
   * @param Quotes
   * @param epExpr
   * @param annList
   * @param paramNameValueLookup
   * @param successCallback - on when the params/values length matched.
   *                        Query Params are those not found in the annotated path
   * @tparam A - Expr of an Annotated Essential Action or its subtye
   * @tparam P - Computed annotated path
   * @return
   */
  private def getAnnotatedPath[A : Type, P : Type](using Quotes)(
    epExpr: Expr[A],
    annList: List[quotes.reflect.Term],
    paramNameValueLookup: Map[String, quotes.reflect.Term],
    successCallback: (Expr[String], Expr[List[Any]], Expr[List[String]], Expr[List[Any]]) =>  Expr[P],
    supportQueryStringPost: Boolean
  ): Expr[P] =
    import quotes.reflect.*
    extractAnnotations(annList) match
      case None =>
        report.errorAndAbort(s"No annotated path found ${epExpr}", epExpr)

      case Some((method: String, declaredPath: String)) =>
        /*
         * update the del
         */
        val (computedPath: List[Expr[Any]], queryParamKeys: List[String], queryParamValues: List[Expr[Any]]) =
          getComputedPathExpr(epExpr, paramNameValueLookup, declaredPath)
        if  !supportQueryStringPost && method.equalsIgnoreCase("Post") && queryParamKeys.nonEmpty then {
          report.errorAndAbort("Query String for Post method is not supported")
        } else ()

        successCallback(Expr(method), Expr.ofList(computedPath), Expr[List[String]](queryParamKeys), Expr.ofList(queryParamValues))

  /**
   * Search for the inner most Term, skip all the outer Inlined
   * @param Quotes
   * @param term
   * @param level
   * @return
   */
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
  private def extractAnnotations(using Quotes)(annList: List[quotes.reflect.Term]): Option[(String, String)] =
    import quotes.reflect.*
    show("AnnotationList", annList)
    val xs = annList.collect {
      case Apply(Select(New(annMethod), _), args) =>
        show("Annotation HttpMethod", annMethod, true)
        show("Annotation Path Parts", args, true)
        args.collectFirst {
          case Literal(c) => (annMethod.symbol.name, c.value.toString)
          case Wildcard() => (annMethod.symbol.name, "")
        }
    }.flatten
    if xs.isEmpty then None
    else
      val tup = xs.foldLeft(("", "")){(res, tup2) =>
        tup2 match {
          case ("Path", path) if path.nonEmpty => (res._1, path)
          case (method, path) if path.isEmpty => (method, res._2)
          case (method, path) => (method, path)
        }
      }
      Some(tup)

  private def getComputedPathExpr[A : Type](using Quotes)(
    actionExpr: Expr[A],
    paramNameValueLookup: Map[String, quotes.reflect.Term],
    declaredPath: String): (List[Expr[Any]], List[String], List[Expr[Any]])  = {

    import quotes.reflect.*

    //Parameterized Path
    var usedPathParamNames: List[String] = Nil

    def getPathParamExpr(name: String): Expr[Any] =
      paramNameValueLookup.get(name) match
        case Some(value) =>
          usedPathParamNames = usedPathParamNames :+ name
          value match {
            case Literal(c: StringConstant)  =>
              //UrlEncode for all String value
              Expr(urlEncode(c.value.asInstanceOf[String]))

            case valOrDef if valOrDef.symbol.isValDef || valOrDef.symbol.isDefDef =>
              //UrlEncode all the Idents of type String
              valOrDef.tpe.asType match {
                case '[String] =>
                  '{urlEncode(${valOrDef.asExprOf[String]})}
                case _ =>
                  valOrDef.asExpr
              }

            case x =>
              x.asExpr
          }
        case None =>
          report.errorAndAbort(s"Path param [$name] of path [$declaredPath] cannot be found in method's param names [${paramNameValueLookup.keySet.mkString(",")}]  ", actionExpr)

    def paramPathExtractor(path: String): List[Expr[Any]] =
      val parts = path.split("/:")
      parts.tail.foldLeft(List[Expr[Any]](Expr(parts.head))) { (accPath, part) =>
        val newParts = part.split("/").toList match
          case Nil =>  Nil
          case xs => getPathParamExpr(xs.head) +: xs.tail.map(p => Expr(p))

        accPath ++ newParts
      }

    //compute Path
    val computedPath: List[Expr[Any]] =
      declaredPath match {
        case path if path.startsWith("prefix:/") =>
          List(Expr(path.replaceFirst("prefix:","")))
        case path if path.matches("regex:\\^?.+") =>
          val paramPath =
            path
              .replaceAll("(\\(\\?-?[idmsuxU]\\))", "") //remove all regex 'on' and 'off' modifiers
              .replaceFirst("regex:\\^?([^$]+)\\$?", "$1") //remove 'regex:' + optional '^' + optional '?'
              .replaceAll("/\\(\\?<(\\w+)>.+?\\)", "/:$1") //replace regex-param with :param

          paramPathExtractor(paramPath)

// FIXME        case path if path.startsWith("glob:/") =>
//          scala.compiletime.error("glob: is not supported")

        case path =>
          //convert all braced params to colon params
          val _path=path.replaceAll("/\\{(\\w+)}", "/:$1")
          paramPathExtractor(_path)
      }

    //compute QueryString
    val queryParamKeys: List[String] = paramNameValueLookup.keys.toList diff usedPathParamNames
    val queryParamValues: List[Expr[Any]] = queryParamKeys.map{k =>
      paramNameValueLookup(k) match
        case Literal(c: StringConstant) =>
          //UrlEncode for all String value
          Expr(urlEncode(c.value.asInstanceOf[String]))

        case valOrDef if valOrDef.symbol.isValDef || valOrDef.symbol.isDefDef =>
          //UrlEncode all the Idents of type String
          valOrDef.tpe.asType match {
            case '[String] =>
              '{urlEncode($ {valOrDef.asExprOf[String]})}

            case _ =>
              valOrDef.asExpr
          }

        case x =>
          x.asExpr
    }

    (computedPath, queryParamKeys, queryParamValues)
  }

}
