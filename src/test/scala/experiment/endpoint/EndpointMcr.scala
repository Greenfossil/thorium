package experiment.endpoint

import com.greenfossil.thorium.{*, given}

inline def EndpointMcr[A <: EssentialAction](inline action: A): Endpoint =
  ${ EndpointMcrImpl( '{action} ) }

import scala.quoted.*
def EndpointMcrImpl[A <: EssentialAction : Type](actionExpr: Expr[A])(using Quotes): Expr[Endpoint] =
  import AnnotatedPathMcr.*

  computeActionAnnotatedPath(
    actionExpr,
    (method, exprList) => '{ Endpoint(${exprList}.mkString("/")) }
  )