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
import io.github.yskszk63.jnhttpmultipartformdatabodypublisher.MultipartFormDataBodyPublisher

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

class Recaptcha_Post_Multipart_Stress_Suite extends munit.FunSuite:

  private def startServer(addRecaptchaGuardModule: Boolean = true): Server =
    val server = Server(0)
      .addServices(RecaptchaServices)
      .addThreatGuardModule( if !addRecaptchaGuardModule then null else RecaptchaGuardModule(RecaptchaMainTestService.recaptchaPathVerificationFn))
    server.start()

  test("/recaptcha/multipart-form with RecaptchaGuardModule"):
    val server = startServer(addRecaptchaGuardModule = true)

    val n = 5
    val xs = 1 to n map { i =>
      val mpPub = MultipartFormDataBodyPublisher().add("g-recaptcha-response", "bad-code")
      val resp = HttpClient.newHttpClient()
        .send(
          HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/recaptcha/multipart-form"))
            .POST(mpPub)
            .header("Content-Type", mpPub.contentType())
            .build(),
          HttpResponse.BodyHandlers.ofString()
        )
      assertEquals(resp.statusCode(), 401)
      assertNoDiff(resp.body(),  """|<!DOCTYPE html>
                                    |<html>
                                    |<head>
                                    |  <title>Unauthorized Access</title>
                                    |</head>
                                    |<body>
                                    |  <h1>Access Denied</h1>
                                    |</body>
                                    |</html>
                                    |""".stripMargin)
    }

    assertEquals(xs.size, n)

    server.stop()

