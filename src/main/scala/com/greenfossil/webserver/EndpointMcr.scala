package com.greenfossil.webserver

inline def EndpointMcr[A <: EssentialAction](inline action: A): Endpoint =
  ${ EndpointMcrImpl( '{action} ) }

import scala.quoted.*
def EndpointMcrImpl[A <: EssentialAction : Type](actionExpr: Expr[A])(using Quotes): Expr[Endpoint] =
  import AnnotatedPathMacroSupport.*

  computeActionAnnotatedPath(
    actionExpr,
    (exprMethod, exprPathPartList, exprQueryParamKeys, exprQueryParamValues) =>
      '{
        Endpoint(
          ${exprPathPartList}.mkString("/"),
          ${exprMethod},
          ${exprQueryParamKeys}.zip(${exprQueryParamValues})
        )
      }
  )