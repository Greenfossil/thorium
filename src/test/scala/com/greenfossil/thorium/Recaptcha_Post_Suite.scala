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
import com.linecorp.armeria.client.{RequestOptions, WebClient}
import com.linecorp.armeria.common.multipart.{BodyPart, Multipart}
import com.linecorp.armeria.common.{ContentDisposition, HttpData, HttpStatus}

import java.nio.charset.StandardCharsets

class Recaptcha_Post_Suite extends munit.FunSuite:

  private def startServer(addRecaptchaGuardModule: Boolean = true): Server =
    val server = Server(0)
      .addServices(RecaptchaServices)
      .addThreatGuardModule( if !addRecaptchaGuardModule then null else RecaptchaGuardModule(RecaptchaMain.recaptchaPathVerificationFn))
    server.start()

  test("/recaptcha/form"):
    val server = startServer()
    val response= WebClient.of().post(s"http://localhost:${server.port}/recaptcha/form", HttpData.empty())
      .aggregate()
      .join()

    server.stop()
    assertEquals(response.status(), HttpStatus.UNAUTHORIZED)
    assertNoDiff(response.contentUtf8(), "Recaptcha exception - No recaptcha token found")

  test("/recaptcha/form - with an invalid g-recaptcha-response".ignore):
    val server = startServer()
    val response = WebClient.of().post(s"http://localhost:${server.port}/recaptcha/form", HttpData.of(StandardCharsets.UTF_8, "g-recaptcha-response=bad-code"))
      .aggregate()
      .join()

    server.stop()
    assertEquals(response.status(), HttpStatus.UNAUTHORIZED)
    assertNoDiff(response.contentUtf8(), """Unauthorize access - Recaptcha({"success":false,"error-codes":["invalid-input-response"]})""")

  test("/recaptcha/guarded-form with RecaptchaGuardModule"):
    val server = startServer()
    val response = WebClient.of().post(s"http://localhost:${server.port}/recaptcha/guarded-form", HttpData.of(StandardCharsets.UTF_8, "g-recaptcha-response=bad-code"))
      .aggregate()
      .join()

    server.stop()
    assertEquals(response.status(), HttpStatus.UNAUTHORIZED)
    assertNoDiff(response.contentUtf8(), """|<!DOCTYPE html>
                                            |<html>
                                            |<head>
                                            |  <title>Unauthorized Access</title>
                                            |</head>
                                            |<body>
                                            |  <h1>Access Denied</h1>
                                            |</body>
                                            |</html>
                                            |""".stripMargin)

  test("/recaptcha/guarded-form with no RecaptchaGuardModule"):
    val server = startServer(false)
    val response = WebClient.of().post(s"http://localhost:${server.port}/recaptcha/guarded-form", HttpData.of(StandardCharsets.UTF_8, "g-recaptcha-response=bad-code"))
      .aggregate()
      .join()

    server.stop()
    assertEquals(response.status(), HttpStatus.SEE_OTHER)
    assertNoDiff(response.contentUtf8(), "")


  test("/recaptcha/multipart-form with RecaptchaGuardModule"):
    val server = startServer()

    val formPart = BodyPart.of(ContentDisposition.of("form-data", "g-recaptcha-response"), "bad-code")
    val multipart = Multipart.of(formPart)
    val multipartRequest = multipart.toHttpRequest(s"http://localhost:${server.port}/recaptcha/guarded-form")
    val response = WebClient.of().execute(multipartRequest, RequestOptions.builder().responseTimeoutMillis(5000).build())
      .aggregate()
      .join()

    server.stop()
    assertEquals(response.status(), HttpStatus.UNAUTHORIZED)
    assertNoDiff(response.contentUtf8(),
      """|<!DOCTYPE html>
         |<html>
         |<head>
         |  <title>Unauthorized Access</title>
         |</head>
         |<body>
         |  <h1>Access Denied</h1>
         |</body>
         |</html>
         |""".stripMargin)


  test("/recaptcha/multipart-form without RecaptchaGuardModule"):
    val server = startServer(addRecaptchaGuardModule = false)
    val formPart = BodyPart.of(ContentDisposition.of("form-data", "g-recaptcha-response"), "bad-code")
    val multipart = Multipart.of(formPart)
    val multipartRequest = multipart.toHttpRequest(s"http://localhost:${server.port}/recaptcha/guarded-form")
    val response = WebClient.of().execute(multipartRequest, RequestOptions.builder().responseTimeoutMillis(5000).build())
      .aggregate()
      .join()

    server.stop()
    assertEquals(response.status(), HttpStatus.SEE_OTHER)
    assertNoDiff(response.contentUtf8(),"")

