package com.greenfossil.webserver

inline def EndpointMcr[A <: EssentialAction](inline action: A): Endpoint =
  ${ EndpointMcrImpl( '{action} ) }

import scala.quoted.*
def EndpointMcrImpl[A <: EssentialAction : Type](actionExpr: Expr[A])(using Quotes): Expr[Endpoint] =
  import AnnotatedPathMacroSupport.*

  computeActionAnnotatedPath(
    actionExpr,
    (exprMethod, exprPathPartList, exprQueryParamKeys, exprQueryParamValues) =>
      '{ Endpoint(${exprPathPartList}.mkString("/") +
        ${exprQueryParamKeys}.headOption.map(_ => "?").getOrElse("") +
        ${exprQueryParamKeys}
          .zip(${exprQueryParamValues})
          .map(kv => s"${kv._1}=${kv._2}")
          .mkString("&") ,
        ${exprMethod})
      }
  )