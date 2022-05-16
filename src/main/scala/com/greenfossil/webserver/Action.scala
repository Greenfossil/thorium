package com.greenfossil.webserver

import com.linecorp.armeria.common.{HttpRequest, HttpResponse}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

trait Controller

type ActionResponse =  HttpResponse | Result | String

trait EssentialAction extends HttpService:
  protected val httpServiceLogger = LoggerFactory.getLogger("http-service")

trait Action(val fn: Request => ActionResponse) extends EssentialAction:
  override def serve(ctx: ServiceRequestContext, httpRequest: HttpRequest): HttpResponse =
    ArmeriaConverters.toResponse(Action.this, ctx)

object Action:
  def apply(fn: Request => HttpResponse | Result | String): Action =
    new Action(fn) {}

  //TODO - need to add test cases
  def async(fn: Request => Result)(using executor: ExecutionContext): Future[Action] =
    Future(new Action(fn) {})
