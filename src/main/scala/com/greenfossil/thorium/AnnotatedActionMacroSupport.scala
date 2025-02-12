/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.greenfossil.thorium

object AnnotatedActionMacroSupport:
  import quoted.*

  def computeActionAnnotatedPath[A: Type, P: Type](epExpr: Expr[A],
                                                   onSuccessCallback: (Expr[String], Expr[List[Any]], Expr[List[String]], Expr[List[Any]], Expr[String]) => Expr[P],
                                                   supportQueryStringPost: Boolean = true
                                                  )(using q: Quotes): Expr[P] =
    verifyActionType(epExpr)
    val (methodAnnotations, paramValueLookup) = extractActionAnnotations(epExpr)
    constructActionURLPath(epExpr, methodAnnotations, paramValueLookup, onSuccessCallback, supportQueryStringPost)

  /**
   * Allow only EssentialAction, Function and String as value
   * @param epExpr
   * @param q
   * @tparam A
   * @return
   */
  def verifyActionType[A: Type](epExpr: Expr[A])(using q: Quotes): Expr[A] = {
    import q.reflect.*
    epExpr match
      case '{ $a: EssentialAction } => ()
      case '{ $s: String } => () //Allow the use of String di
      case _ =>
        // Recursively unwrap term to locate the actual lamda term.
        def verifyIsLamda(aTerm: Term): Term = aTerm match
          case Inlined(_, _, underlying) => verifyIsLamda(underlying)
          case Lambda(_, _) => aTerm
          case Apply(_, _) => aTerm
          case error => report.errorAndAbort(s"Only EssentialAction, String or Function are allowed as value -  type: ${error.show}", epExpr)
        verifyIsLamda(epExpr.asTerm)

    epExpr
  }

  /**
   * Get a flatten list of param values from the a list of list of param values
   *
   * @param Quotes
   * @param applyTerm
   * @return
   */
  def getFlattenedParamValues(using q:Quotes)(applyTerm: q.reflect.Term): List[q.reflect.Term] =
    import q.reflect.*
    applyTerm match
      case Apply(aTerm: Apply, paramValues) => getFlattenedParamValues(aTerm) ++ paramValues
      case Apply(_, paramValues) => paramValues
      case _ => Nil

  def extractActionAnnotations[A: Type](epExpr: Expr[A])(using q: Quotes): (List[q.reflect.Term], Map[String, q.reflect.Term] ) = {
    import q.reflect.*

    // Recursively unwrap term to locate the actual method symbol.
    def extractMethodTerm(aTerm: Term): Term = aTerm match
      case Inlined(_, _, underlying) => extractMethodTerm(underlying)
      case Lambda(_, body) => extractMethodTerm(body)
      case Apply(fun, _) => aTerm
      case Select(select, _) => aTerm
      case _ => aTerm

    val methodTerm = extractMethodTerm(epExpr.asTerm)
    val methodTermSym = methodTerm.symbol

    // Retrieve the annotations from the resolved method symbol.
    //NB: For explicit endpoint using String, no method will be available
    val methodAnnotationList: List[Term] = methodTermSym.annotations

    /*Show annotationList
    println("* Method annotations: " + methodAnnotationList.size)
    methodAnnotationList.foreach { term => println(term.show(using Printer.TreeAnsiCode)) }*/

    //Get all param names annotated with @Param from all paramList
    import com.linecorp.armeria.server.annotation.Param
    val annotatedParamNames: List[String] = methodTermSym.paramSymss.flatten.collect {
      case sym: Symbol if sym.annotations.exists(_.symbol.fullName.startsWith(classOf[Param].getName)) =>
        sym.name
    }
    /*println(s"annotatedParamNames:${annotatedParamNames.size} - ${annotatedParamNames.mkString("[", ",", "]")}")*/

    //Get all param names from all paramList
    val paramNames: List[String] = methodTermSym.paramSymss.flatten.map(_.name)

    //Removed the params that are not annotated with @Param
    val paramValues = getFlattenedParamValues(methodTerm)
    /*println(s"paramValues:${paramValues.size}")
    paramValues.foreach(value => println("value:" + value.show(using Printer.TreeAnsiCode)) )*/

    val paramNameValueLookup: Map[String, Term] =
      paramNames.zip(paramValues).toMap.filter((key, _) => annotatedParamNames.contains(key))

    /*println(s"paramNameValueLookup:${paramNameValueLookup.size}")
    paramNameValueLookup.foreach{ (key, value) =>
      println(s"paramNameValue: ${key}, value:${value.show(using Printer.TreeStructure)}")
    }*/
    (methodAnnotationList, paramNameValueLookup)
  }

  def constructActionURLPath[A : Type, P : Type](using q: Quotes)(
    epExpr: Expr[A],
    methodAnnotations: List[q.reflect.Term],
    paramValueLookup: Map[String, q.reflect.Term],
    successCallback: (Expr[String], Expr[List[Any]], Expr[List[String]], Expr[List[Any]], Expr[String]) =>  Expr[P],
    supportQueryStringPost: Boolean
  ): Expr[P] =
    extractHttpVerb(methodAnnotations) match
      case None /*String Endpoint*/=>
        successCallback(Expr("Get"), Expr.ofList(List(epExpr)), Expr[List[String]](Nil), Expr.ofList(List.empty[Expr[Any]]), Expr(""))

      case Some((method: String, pathPattern: String)) =>
        val (computedPath, queryParamKeys, queryParamValues) = buildPathAndParts(epExpr, paramValueLookup , pathPattern)

        import q.reflect.*
        if !supportQueryStringPost && method.equalsIgnoreCase("Post") && queryParamKeys.nonEmpty
        then report.errorAndAbort("Query String for Post method is not supported", epExpr)
        end if
        successCallback(Expr(method), Expr.ofList(computedPath), Expr[List[String]](queryParamKeys), Expr.ofList(queryParamValues), Expr(pathPattern))

  def extractHttpVerb(using q:Quotes)(methodAnnotationList: List[q.reflect.Term]): Option[(String, String)] =
    import q.reflect.*

    def getAnnotationValue(args: List[Term], default: String): String =
      args.headOption.flatMap {
        case NamedArg("value", value) => value.asExprOf[String].value
        case _ => None
      }.getOrElse(default)

    val (httpVerb, pathParams) =
      import com.linecorp.armeria.server.annotation.{Get, Head, Path, Post, Put, Patch, Delete, Trace, Options}
      methodAnnotationList.foldLeft(("", "")) { (acc, methodAnn) =>
        methodAnn match
          case Apply(Select(New(ann), _), args) =>
            val (_httpVerb, _pathParams) = acc
            val method = ann.tpe.asType match {
              case '[Path] => if _httpVerb.isBlank then "Path" else _httpVerb
              case '[Options] => "Options"
              case '[Get] => "Get"
              case '[Head] => "Head"
              case '[Post] => "Post"
              case '[Put] => "Put"
              case '[Patch] => "Patch"
              case '[Delete] => "Delete"
              case '[Trace] => "Trace"
              case _ => ""
            }
            if method == "" then acc else (method, getAnnotationValue(args, _pathParams))
          case _ => acc
      }

    if httpVerb.isBlank then None else Some(httpVerb -> pathParams)

  inline private def urlEncode(str: String): String =
    java.net.URLEncoder.encode(Option(str).getOrElse(""), java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20")

  /**
   * URL encode for string value
   * @param q
   * @param paramValue
   * @return
   */
  def urlencodeStringExpr(using q: Quotes)(paramValue: q.reflect.Term): Expr[?] =
    paramValue.asExpr match
      case '{ $s: String } => '{ urlEncode($s) }
      case x => x

  def convertPathToPathParts(pathPattern: String): List[String] = {
    def paramPathExtractor(path: String): List[String] =
      val pathParamName = path.split("/:")
      pathParamName.tail.foldLeft(List[String](pathParamName.head)) { (accPath, paramName) =>
        val newParts = paramName.split("/").toList match
          case Nil => Nil
          case head :: tail => (":" + head) +: tail
        end newParts
        accPath ++ newParts
      }

    pathPattern match
      case path if path.startsWith("prefix:/") => List(path.replaceFirst("prefix:", ""))
      case path if path.matches("regex:\\^?.+") =>
        val paramPath =
          path
            .replaceAll("(\\(\\?-?[idmsuxU]\\))", "") //remove all regex 'on' and 'off' modifiers
            .replaceFirst("regex:\\^?([^$]+)\\$?", "$1") //remove 'regex:' + optional '^' + optional '?'
            .replaceAll("/\\(\\?<(\\w+)>.+?\\)", "/:$1") //replace regex-param with :param

        paramPathExtractor(paramPath)
      case path =>
        //convert all braced params to colon params
        val _path = path.replaceAll("/\\{(\\w+)}", "/:$1")
        paramPathExtractor(_path)
  }

  //compute Path
  def computedPathParts(using q: Quotes)(pathPattern: String,  paramAnnotationsLookup: Map[String, q.reflect.Term] ): (List[String], List[Expr[Any]]) = {
    import q.reflect.*

    //Parameterized Path
    var processedPathParamNames: List[String] = Nil

    def getPathParamExpr(name: String): Expr[Any] =
      paramAnnotationsLookup.get(name) match
        case None =>
          report.errorAndAbort(s"Path param [$name] of pathPattern [$pathPattern] cannot be found in method's param names [${paramAnnotationsLookup.keySet.mkString(",")}]  ")
        case Some(paramValue) =>
          processedPathParamNames = processedPathParamNames :+ name
          urlencodeStringExpr(paramValue)

    val pathParts = convertPathToPathParts(pathPattern).map{part =>
      if part.startsWith(":") then getPathParamExpr(part.drop(1) /*drop ':'*/) else Expr(part)
    }

    (processedPathParamNames, pathParts)
  }

  def buildPathAndParts[A: Type](using q:Quotes)(
    actionExpr: Expr[A],
    paramValueLookup: Map[String, q.reflect.Term] ,
    pathPattern: String): (List[Expr[Any]], List[String], List[Expr[Any]]) =

    val (processedPathParamNames, pathParts) = computedPathParts(pathPattern, paramValueLookup)

    //compute QueryString
    val queryParamKeys = paramValueLookup.keys.toList diff processedPathParamNames
    val queryParamValues = queryParamKeys.flatMap( k => paramValueLookup.get(k).map(urlencodeStringExpr) )

    (pathParts, queryParamKeys, queryParamValues)