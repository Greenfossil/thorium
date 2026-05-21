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

import com.linecorp.armeria.common.MediaType

import java.time.Duration

object PreAuthVerificationMainTestService:

  private val bypassConfig = PreAuthVerificationBypassConfiguration(
    enabled = true,
    allowPaths = Seq("/auth/verify-token"),
    allowMethods = Seq("POST"),
    requiredContentTypes = Seq(MediaType.JSON.toString),
    requiredHeaders = Seq("X-Verify-Channel")
  )

  @main
  def preAuthVerificationMain(): Unit =
    val server = Server(8080)
      .setPreAuthVerificationBypass(bypassConfig)
      .addServices(CSRFServices)
      .addCSRFGuard((_ /*origin*/, _ /*referer*/, _ /*ctx*/) => false)
      .serverBuilderSetup(_.requestTimeout(Duration.ofHours(1)))
      .start()

    server.serviceConfigs foreach { c =>
      println(s"c.route() = ${c.route()}")
    }
    println("Server started...")
    println("Manual positive test example:")
    println(
      "curl -i -X POST http://localhost:8080/auth/verify-token " +
        "-H 'Content-Type: application/json' " +
        "-H 'X-Verify-Channel: internet-client' " +
        "--data '{\"token\":\"abc\"}'"
    )

