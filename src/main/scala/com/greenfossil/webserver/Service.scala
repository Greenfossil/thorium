package com.greenfossil.webserver

import com.linecorp.armeria.common.{HttpHeaders, HttpRequest, HttpResponse, ResponseHeaders}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}
import com.linecorp.armeria.server.annotation.ResponseConverterFunction

trait Service(fn: Request => HttpResponse) extends HttpService {
  override def serve(ctx: ServiceRequestContext, req: HttpRequest): HttpResponse =
    val req = new Request(ctx) {}
    HttpResponse.from(() => fn(req), ctx.blockingTaskExecutor())
}

object Service{

  def apply(fn: Request => HttpResponse): HttpService = new Service(fn){}
  
}

