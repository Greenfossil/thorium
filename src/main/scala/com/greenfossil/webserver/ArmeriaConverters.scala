package com.greenfossil.webserver

import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.{RequestConverterFunction, ResponseConverterFunction}

//FIXME - handle ExceptionHandlerFunction
object ArmeriaConverters extends RequestConverterFunction, ResponseConverterFunction {
  import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpHeaders, HttpResponse, ResponseHeaders}
  import java.lang.reflect.ParameterizedType
  import java.util.concurrent.CompletableFuture

  override def convertRequest(ctx: ServiceRequestContext, request: AggregatedHttpRequest,
                              expectedResultType: Class[_],
                              expectedParameterizedResultType: ParameterizedType): AnyRef =
    if expectedResultType == classOf[com.greenfossil.webserver.Request]
    then new com.greenfossil.webserver.Request(ctx, request) {}
    else RequestConverterFunction.fallthrough()

  override def convertResponse(ctx: ServiceRequestContext, headers: ResponseHeaders,
                               result: Any,
                               trailers: HttpHeaders): HttpResponse =
    result match {
      case action: Action => toResponse(action, ctx)
      case _ => ResponseConverterFunction.fallthrough()
    }

  def toResponse(action: Action, reqContext: ServiceRequestContext): HttpResponse =
    val f: CompletableFuture[HttpResponse] = reqContext.request().aggregate().thenApply(aggregateRequest => {
      Thread.currentThread().setContextClassLoader(getClass.getClassLoader)
      val req = new Request(reqContext, aggregateRequest) {}
      action.fn(req) match
        case s: String => HttpResponse.of(s)
        case hr: HttpResponse => hr
        case result:Result => result.toHttpResponse(req)
    })
    HttpResponse.from(f)
}