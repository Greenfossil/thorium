package com.greenfossil.webserver

inline def EndpointMcr[A <: Action](inline action: A): Endpoint =
  ${ EndpointMcrImpl( '{action} ) }

import scala.quoted.*
def EndpointMcrImpl[A <: Action : Type](actionExpr: Expr[A])(using Quotes): Expr[Endpoint] =
  import AnnotatedPathMacroSupport.*

  computeActionAnnotatedPath(
    actionExpr,
    (method, exprList) => '{ Endpoint(${exprList}.mkString("/")) }
  )