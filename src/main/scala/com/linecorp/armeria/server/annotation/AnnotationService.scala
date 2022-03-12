package com.linecorp.armeria.server.annotation

import com.linecorp.armeria.common.{HttpHeaders, HttpResponse, ResponseHeaders}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext}

@ResponseConverter(classOf[AnnotatedHttpServiceResponseConverter])
trait AnnotatedHttpServiceSet

trait AnnotatedHttpService extends HttpService

class AnnotatedHttpServiceResponseConverter extends ResponseConverterFunction:
  override def convertResponse(ctx: ServiceRequestContext, headers: ResponseHeaders, result: Any, trailers: HttpHeaders): HttpResponse =
    result match
      case service: AnnotatedHttpService => service.serve(ctx, ctx.request())
      case _ => ResponseConverterFunction.fallthrough()

