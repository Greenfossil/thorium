/*
 *  Copyright 2022 Greenfossil Pte Ltd
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.greenfossil.thorium

import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.common.ContentDisposition
import com.linecorp.armeria.common.multipart.{BodyPart, Multipart}

class ThreatGuardModule_Stress_Suite extends munit.FunSuite:

  val tokenName = "token-name"
  val tokenValue = "token-value"
  private def startServer: Server =
    val server = Server(0)
      .addServices(ThreatGuardServices)
      .addThreatGuardModule(ThreatGuardTestModule(tokenName, tokenValue))
    server.start()

  test("/recaptcha/multipart-form with RecaptchaGuardModule"):
    val server = startServer

    val n = 50000
    1 to n foreach { i =>
      val formPart = BodyPart.of(ContentDisposition.of("form-data", tokenName), tokenValue)
      val multipart = Multipart.of(formPart)
      val multipartRequest = multipart.toHttpRequest(s"http://localhost:${server.port}/threat-guard/multipart-form")
      val response = WebClient.of().execute(multipartRequest)
        .aggregate()
        .join()
      println(s"response i:$i status:${response.status()}, ${response.contentUtf8()}")
    }

    server.stop()