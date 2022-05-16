package com.greenfossil.webserver

inline def EndpointMcr[A <: EssentialAction](inline action: A): Endpoint =
  ${ EndpointMcrImpl( '{action} ) }

import scala.quoted.*
def EndpointMcrImpl[A <: EssentialAction : Type](actionExpr: Expr[A])(using Quotes): Expr[Endpoint] =
  import AnnotatedPathMacroSupport.*

  computeActionAnnotatedPath(
    actionExpr,
    (method, exprList) => '{ Endpoint(${exprList}.mkString("/")) }
  )