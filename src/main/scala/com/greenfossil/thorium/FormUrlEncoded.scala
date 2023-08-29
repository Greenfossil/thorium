/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greenfossil.thorium

import com.linecorp.armeria.common.{AggregatedHttpRequest, HttpMethod, MediaType}
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.RequestConverterFunction

import java.lang.reflect.ParameterizedType

case class FormUrlEndcoded(form: Map[String, Seq[String]]):
  export form.*
  def getFirst(fieldName: String):Option[String] = 
    get(fieldName).map(_.headOption.orNull)

object FormUrlEncodedRequestConverterFunction extends RequestConverterFunction:
  override def convertRequest(ctx: ServiceRequestContext,
                              request: AggregatedHttpRequest,
                              expectedResultType: Class[?],
                              expectedParameterizedResultType: ParameterizedType): AnyRef =
    if expectedResultType == classOf[FormUrlEndcoded]
      &&  MediaType.FORM_DATA == request.contentType() && HttpMethod.POST == request.method() then
      FormUrlEncodedParser.parse(request.contentUtf8())
    else RequestConverterFunction.fallthrough()