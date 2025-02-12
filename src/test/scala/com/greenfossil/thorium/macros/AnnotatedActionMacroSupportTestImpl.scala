package com.greenfossil.thorium.macros

import com.greenfossil.thorium.*

object AnnotatedActionMacroSupportTestImpl:

  import quoted.*

  inline def verifyActionTypeMcr[A](inline ep: A): A =
    ${ AnnotatedActionMacroSupport.verifyActionType( '{ep} )}

  inline def extractActionAnnotationsMcr[A](inline fnExpr: A): (Int, Int) =
    ${ extractActionAnnotationsImpl( '{ fnExpr } ) }

  def extractActionAnnotationsImpl[A: Type](fnExpr: Expr[A])(using q: Quotes): Expr[(Int, Int)] =
    val (methodAnnotations, paramAnnotations) = AnnotatedActionMacroSupport.extractActionAnnotations(fnExpr)
    Expr( (methodAnnotations.size, paramAnnotations.size) )

  inline def extractHttpVerbMcr[A](inline ep: A): String =
    ${ extractHttpVerbImpl( '{ep} ) }

  def extractHttpVerbImpl[A: Type](epExpr: Expr[A])(using q: Quotes): Expr[String] =
    val (methodAnnotations, paramAnnotations) = AnnotatedActionMacroSupport.extractActionAnnotations(epExpr)
      val (method, pathPattern) = AnnotatedActionMacroSupport.extractHttpVerb(methodAnnotations).getOrElse(("[]", "[]"))
    Expr(s"Method: $method, Path: $pathPattern")

  inline def computeActionAnnotatedPathMcr[A](inline ep:A): Endpoint =
    ${ computeActionAnnotatedPathImpl( '{ep} ) }

  def computeActionAnnotatedPathImpl[A: Type](epExpr: Expr[A])(using q: Quotes): Expr[Endpoint] =
    AnnotatedActionMacroSupport.computeActionAnnotatedPath(
      epExpr = epExpr,
      onSuccessCallback = (exprMethod, exprPathPartList, exprQueryParamKeys, exprQueryParamValues, exprPathPattern) =>
        '{
          Endpoint(
            ${exprPathPartList}.mkString("/"),
            ${exprMethod},
            ${exprQueryParamKeys}.zip(${exprQueryParamValues}),
            Some(${exprPathPattern})
          )
        },
      supportQueryStringPost = true
    )

