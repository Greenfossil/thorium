package com.greenfossil.webserver

import com.linecorp.armeria.common.{HttpResponse, HttpRequest}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}

import scala.concurrent.{ExecutionContext, Future}

trait Controller

trait Action(val fn: Request => HttpResponse | Result | String) extends HttpService:
  override def serve(ctx: ServiceRequestContext, httpRequest: HttpRequest): HttpResponse =
    ArmeriaConverters.toResponse(Action.this, ctx)


object Action:
  def apply(fn: Request => HttpResponse | Result | String): Action =
    new Action(fn) {}

  //TODO - need to add test cases
  def async(fn: Request => Result)(using executor: ExecutionContext): Future[Action] =
    Future(new Action(fn) {})
