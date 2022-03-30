//package com.greenfossil.webserver
//
///**
// * Inline redirect macro
// * @param action
// * @return
// */
//import com.linecorp.armeria.server.annotation.AnnotatedHttpService
//
//inline def RedirectMcr[A <: AnnotatedHttpService](inline action: A): Result =
//  ${ RedirectMcrImpl('action) }
//
///*
// * TODO - need to handle regex params eg. ReportBuilderController.viewReportBuilder -   @Path("/builder/report/reportId<[0-9]+>/:mode")
// * Problem with duplicate paths - @Path("/builder/report/reportId<[0-9]+>/:mode")  @Path("/builder/report/:reportId/:mode")
// * Support declaration of Path from class using type projection - SystemController.triggerMailDiagnostics 
// */
//import scala.quoted.*
//def RedirectMcrImpl[A <: AnnotatedHttpService : Type](actionExpr:Expr[A])(using Quotes): Expr[Result] =
//  import quotes.reflect.*
//  import AnnotatedPathMacroSupport.*
//
//  computeActionAnnotatedPath(
//    actionExpr,
//    (method, exprList) => '{Redirect(${exprList}.mkString("/"))}
//  )
