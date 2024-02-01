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

import java.time.Duration

object CSRFMain:

  @main
  def csrfMain =
    val server = Server(8080)
      .addServices(CSRFServices)
      .addCSRFGuard()
      .serverBuilderSetup(_.requestTimeout(Duration.ofHours(1)))
      .start()

    server.serviceConfigs foreach { c =>
      println(s"c.route() = ${c.route()}")
    }
    println(s"Server started... ${Thread.currentThread()}")