package com.greenfossil.webserver


inline def EndpointMcr[A <: AnnotatedHttpService](inline action: A): Endpoint =
  ${ EndpointMcrImpl( '{action} ) }

import com.linecorp.armeria.server.annotation.AnnotatedHttpService

import scala.quoted.*
def EndpointMcrImpl[A <: AnnotatedHttpService : Type](actionExpr: Expr[A])(using Quotes): Expr[Endpoint] =
  import AnnotatedPathMacroSupport.*

  computeActionAnnotatedPath(
    actionExpr,
    (method, exprList) => '{ Endpoint(${exprList}.mkString("/")) }
  )