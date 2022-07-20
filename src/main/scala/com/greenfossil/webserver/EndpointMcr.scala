package com.greenfossil.webserver

inline def EndpointMcr[A](inline ep: A): Endpoint =
  ${ EndpointMcrImpl( '{ep} ) }

import scala.quoted.*
def EndpointMcrImpl[A : Type](epExpr: Expr[A])(using Quotes): Expr[Endpoint] =
  import AnnotatedPathMacroSupport.*

  computeActionAnnotatedPath(
    epExpr,
    (exprMethod, exprPathPartList, exprQueryParamKeys, exprQueryParamValues) =>
      '{
        Endpoint(
          ${exprPathPartList}.mkString("/"),
          ${exprMethod},
          ${exprQueryParamKeys}.zip(${exprQueryParamValues})
        )
      }
  )