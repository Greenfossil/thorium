package com.greenfossil.webserver

import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpData, HttpRequest, HttpResponse}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}

import java.time.Duration
import java.util.concurrent.CompletableFuture
import scala.concurrent.{ExecutionContext, Future}

trait Controller

type ActionResponse =  HttpResponse | Result | String | Array[Byte]

trait EssentialAction extends HttpService:
  /**
   * Armeria invocation during an incoming request
   * @param svcRequestContext
   * @param httpRequest
   * @return
   */
  override def serve(svcRequestContext: ServiceRequestContext, httpRequest: HttpRequest): HttpResponse =
    val future = svcRequestContext.request().aggregate()
    println(s"future.isDone = ${future.isDone}")
    val f: CompletableFuture[HttpResponse] =
      if !future.isDone then {
        println(s"Processing not done future")
        future.thenApply(aggregateRequest => invokeAction(svcRequestContext, aggregateRequest))
      } else {
        println("Processing is done future")
        val resp = invokeAction(svcRequestContext, future.get())
        CompletableFuture.completedFuture(resp)
      }
    HttpResponse.from(f)

  private def invokeAction(svcRequestContext:ServiceRequestContext, aggregateRequest: AggregatedHttpRequest): HttpResponse = {
    val req = new Request(svcRequestContext, aggregateRequest) {}
    apply(req) match
      case s: String => HttpResponse.of(s)
      case hr: HttpResponse => hr
      case result: Result => result.toHttpResponse(req)
      case bytes: Array[Byte] => HttpResponse.of(HttpData.wrap(bytes))
  }

  /**
   * This method is only invoked EssentialAction.serve
   * @param request
   * @return
   */
  protected def apply(request: Request): ActionResponse

end EssentialAction

trait Action extends EssentialAction

object Action:

  def apply(fn: Request => ActionResponse): Action = (request: Request) => fn(request)

  //TODO - need to add test cases
  def async(fn: Request => ActionResponse)(using executor: ExecutionContext): Future[Action] =
    Future((request: Request) => fn(request))
