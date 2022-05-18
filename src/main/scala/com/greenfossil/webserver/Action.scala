package com.greenfossil.webserver

import com.linecorp.armeria.common.{HttpRequest, HttpResponse}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import org.slf4j.LoggerFactory

import java.util.concurrent.CompletableFuture
import scala.concurrent.{ExecutionContext, Future}

trait Controller

type ActionResponse =  HttpResponse | Result | String

trait EssentialAction extends HttpService:
  /**
   * Armeria invocation during an incoming request
   * @param ctx
   * @param httpRequest
   * @return
   */
  override def serve(ctx: ServiceRequestContext, httpRequest: HttpRequest): HttpResponse =
    val f: CompletableFuture[HttpResponse] = ctx.request().aggregate().thenApply(aggregateRequest => {
      Thread.currentThread().setContextClassLoader(getClass.getClassLoader)
      val req = new Request(ctx, aggregateRequest) {}
      apply(req) match
        case s: String => HttpResponse.of(s)
        case hr: HttpResponse => hr
        case result:Result => result.toHttpResponse(req)
    })
    HttpResponse.from(f)

  inline def url:String = Endpoint.apply(this).url

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
