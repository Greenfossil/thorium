package com.greenfossil.webserver

inline def EndpointFnMcr[A](inline fn: A): Endpoint =
  ${ EndpointFnMcrImpl( '{fn} ) }

import scala.quoted.*
def EndpointFnMcrImpl[A : Type](fnExpr: Expr[A])(using Quotes): Expr[Endpoint] =
  import AnnotatedPathMacroSupport2.*

  computeActionAnnotatedPath(
    fnExpr,
    (exprMethod, exprPathPartList, exprQueryParamKeys, exprQueryParamValues) =>
      '{
        Endpoint(
          ${exprPathPartList}.mkString("/"),
          ${exprMethod},
          ${exprQueryParamKeys}.zip(${exprQueryParamValues})
        )
      }
  )