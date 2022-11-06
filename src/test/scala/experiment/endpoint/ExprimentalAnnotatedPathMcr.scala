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

package experiment.endpoint

import com.greenfossil.thorium.{*, given}

@deprecated("to be removed")
object ExprimentalAnnotatedPathMcr extends MacroSupport(globalDebug = false) {

  import scala.quoted.*

  def computeActionAnnotatedPath[A <: EssentialAction : Type, R : Type](actionExpr: Expr[A],
                                                                        onSuccessCallback: (String, Expr[List[Any]], String) =>  Expr[R])
                                                                       (using Quotes): Expr[R] =
    import quotes.reflect.*
    searchForAnnotations(actionExpr.asTerm, 1) match
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
        show("otherTerm", otherTerm, true)
        val owner = Symbol.spliceOwner
        show("owner", owner.tree)
        report.errorAndAbort("Unable to find any Essential Action path annotations")


  private def getAnnotatedPath[A <: EssentialAction : Type, R : Type](using Quotes)(
    actionExpr: Expr[A],
    annList: List[quotes.reflect.Term],
    paramNameValueLookup: Map[String, quotes.reflect.Term],
    successCallback: (String, Expr[List[Any]], String) =>  Expr[R]
  ): Expr[R] =
    import quotes.reflect.*
    getDeclaredPath(annList) match
      case None =>
        report.errorAndAbort(s"No annotated path found ${actionExpr}", actionExpr)

      case Some(declaredPath) if paramNameValueLookup.isEmpty =>
        //Constant Path
        successCallback("POST", Expr(List(declaredPath)), declaredPath)

      case Some(declaredPath) =>
        getComputedPathExpr(actionExpr, paramNameValueLookup, declaredPath) match
          case (computedPathExpr, Nil, pathPattern) =>
            successCallback("POST", computedPathExpr, pathPattern)

          case (_, mismatchParams, declaredPath) =>
            report.errorAndAbort(s"Annotated endpoint [${declaredPath}] has params missing [${mismatchParams.mkString(",")}]", actionExpr)

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

  private def getDeclaredPath(using Quotes)(annList: List[quotes.reflect.Term]): Option[String] = {
    import quotes.reflect.*
    annList.collectFirst {
      case Apply(Select(New(annMethod), _), args) =>
        show("Annotation HttpMethod", annMethod)
        show("Annotation Path Parts", args)
        args.collectFirst { case Literal(c) => c.value.toString }
    }.flatten
  }

  private def getComputedPathExpr[A <: EssentialAction : Type](using Quotes)(
    actionExpr: Expr[A],
    paramNameValueLookup: Map[String, quotes.reflect.Term],
    declaredPath: String): (Expr[List[Any]], Seq[String], String)  = {

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
          case Nil =>  Nil
          case xs => getPathParamExpr(xs.head) +: xs.tail.map(p => Expr(p))

        accPath ++ newParts
      }

    val mismatchParams: Seq[String] = paramNameValueLookup.keys.toList diff usedPathParamNames
    val computedPathExpr: Expr[List[Any]] = Expr.ofList(computedPath)
    (computedPathExpr, mismatchParams, declaredPath)
  }

}
