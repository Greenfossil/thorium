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

import com.linecorp.armeria.common.AggregatedHttpRequest
import com.linecorp.armeria.server.ServiceRequestContext

final class MultipartRequest(val multipartFormData: MultipartFormData, 
                             requestContext: ServiceRequestContext, 
                             aggregatedHttpRequest: AggregatedHttpRequest) extends Request(requestContext, aggregatedHttpRequest):
  
  export multipartFormData.{asFormUrlEncoded as _,  *}

  override def asFormUrlEncoded: FormUrlEndcoded = multipartFormData.asFormUrlEncoded

