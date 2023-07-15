package com.greenfossil.thorium

import com.linecorp.armeria.common.AggregatedHttpRequest
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.RequestConverterFunction

import java.lang.reflect.ParameterizedType

case class FormUrlEndcoded(form: Map[String, Seq[String]]):
  export form.*

object FormUrlEncodedConverter extends RequestConverterFunction:
  override def convertRequest(ctx: ServiceRequestContext,
                              request: AggregatedHttpRequest,
                              expectedResultType: Class[?],
                              expectedParameterizedResultType: ParameterizedType): AnyRef =
    if expectedResultType == classOf[FormUrlEndcoded] then
      FormUrlEncodedParser.parse(request.contentUtf8())
    else RequestConverterFunction.fallthrough()