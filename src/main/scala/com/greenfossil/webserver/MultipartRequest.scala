package com.greenfossil.webserver

import com.linecorp.armeria.common.AggregatedHttpRequest
import com.linecorp.armeria.server.ServiceRequestContext

final class MultipartRequest(val multipartFormData: MultipartFormData, requestContext: ServiceRequestContext, aggregatedHttpRequest: AggregatedHttpRequest) extends Request(requestContext, aggregatedHttpRequest) {
  export multipartFormData.{asFormUrlEncoded as _,  *}

  override def asFormUrlEncoded: Map[String, Seq[String]] = multipartFormData.asFormUrlEncoded

}
