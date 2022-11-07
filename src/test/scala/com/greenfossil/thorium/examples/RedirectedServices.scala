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

package com.greenfossil.thorium.examples

import com.linecorp.armeria.server.annotation.{Get, Param}
import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.common.{HttpRequest, HttpResponse, HttpStatus}
import com.linecorp.armeria.server.{RoutingContext, ServiceConfig, ServiceRequestContext}

object RedirectedServices:

  @Get("/s0")
  def s0 = Action { request =>
    println(RedirectedServices2.s3.endpoint.prefixedUrl(using request))
    Redirect( s1("howdy").endpoint.prefixedUrl(using request))
  }

  @Get("/s1/:name")
  def s1(@Param name: String) = Action { request =>
    s"Hello ${name}"
  }
