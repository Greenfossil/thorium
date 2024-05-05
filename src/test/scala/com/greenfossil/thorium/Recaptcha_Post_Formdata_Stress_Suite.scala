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

import com.greenfossil.thorium.decorators.RecaptchaGuardModule
import com.linecorp.armeria.common.MediaType

import java.net.http.{HttpClient, HttpResponse}
import java.net.{URI, http}
import java.time.Duration


class Recaptcha_Post_Formdata_Stress_Suite extends munit.FunSuite:

  private def startServer(addRecaptchaGuardModule: Boolean = true): Server =
    val server = Server(0)
      .addServices(RecaptchaServices)
      .addThreatGuardModule( if !addRecaptchaGuardModule then null else RecaptchaGuardModule(RecaptchaMainTestService.recaptchaPathVerificationFn))
    server.start()

  test("/recaptcha/guarded-form with RecaptchaGuardModule"):
    val server = startServer(addRecaptchaGuardModule = true)

    val n = 5
    val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    val xs = 1 to n map {i =>
      val req = http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/recaptcha/guarded-form"))
        .POST(http.HttpRequest.BodyPublishers.ofString("g-recaptcha-response=bad-code"))
        .header("content-type", MediaType.FORM_DATA.toString)
        .build()
      val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
      assertEquals(resp.statusCode(), 401)
      i
    }

    assertEquals(xs.size, n)

    server.stop()

