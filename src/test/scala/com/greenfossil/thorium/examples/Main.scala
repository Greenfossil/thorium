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

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.common.{HttpHeaderNames, SessionProtocol}
import com.linecorp.armeria.server.ClientAddressSource

@main def main =
  val server = Server(8080)
    .addHttpService("/simpleHttpService", Action{ request =>
      Ok(s"Howdy! env:${request.env.mode}")
    })
    .addServices(BasicServices, FormServices, SimpleServices, ParameterizedServices)
    .addServices(MultipartServices)
    .addDocService()
    .addServices(RedirectedServices2)
    .serverBuilderSetup(sb => {
      sb
        .serviceUnder("/docs", new com.linecorp.armeria.server.docs.DocService())
        .serviceUnder("/api", RedirectedServices2.s3)
        .annotatedService("/ext", RedirectedServices2)
        .annotatedService(RedirectedServices)
        .annotatedService("/api", RedirectedServices)
    })
    .start(SessionProtocol.PROXY)

  server.serviceConfigs foreach { c =>
    println(s"c.route() = ${c.route()}")
  }
  println(s"Server started... ${Thread.currentThread()}")