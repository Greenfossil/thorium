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

@main def main =
  val server = Server(8080)
    .addHttpService("/simpleHttpService", Action{ request =>
      Ok(s"Howdy! env:${request.env.mode}")
    })
    .addServices(BasicServices, FormServices, SimpleServices, ParameterizedServices)
    .addServices(MultipartServices)
    .addDocService()
    .serverBuilderSetup(sb => {
      sb.serviceUnder("/docs", new com.linecorp.armeria.server.docs.DocService())
    })
    .start()
  println(s"Server started... ${Thread.currentThread()}")