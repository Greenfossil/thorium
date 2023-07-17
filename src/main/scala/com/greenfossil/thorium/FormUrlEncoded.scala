package com.greenfossil.thorium

import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpMethod, MediaType}
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.RequestConverterFunction

import java.lang.reflect.ParameterizedType

case class FormUrlEndcoded(form: Map[String, Seq[String]]):
  export form.*

object FormUrlEncodedRequestConverterFunction extends RequestConverterFunction:
  override def convertRequest(ctx: ServiceRequestContext,
                              request: AggregatedHttpRequest,
                              expectedResultType: Class[?],
                              expectedParameterizedResultType: ParameterizedType): AnyRef =
    if expectedResultType == classOf[FormUrlEndcoded]
      &&  MediaType.FORM_DATA == request.contentType() && HttpMethod.POST == request.method() then
      FormUrlEncodedParser.parse(request.contentUtf8())
    else RequestConverterFunction.fallthrough()