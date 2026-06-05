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
import com.typesafe.config.ConfigFactory

import java.net.{URI, http}
import java.time.Duration

class CSRFGuardModule_PreAuthVerificationBypass_Suite extends munit.FunSuite:

  test("Configuration parses preAuthVerificationBypass settings") {
    val config = ConfigFactory
      .parseString(
        """
          |app.http.csrf.preAuthVerificationBypass {
          |  enabled = true
          |  allowPaths = ["/auth/verify-token"]
          |  allowMethods = ["POST"]
          |  requiredContentTypes = ["application/json"]
          |  requiredHeaders = ["X-Verify-Channel"]
          |  requiredHeaderValues = {
          |    "X-Verify-Channel": "internet-client"
          |  }
          |}
          |""".stripMargin
      )
      .withFallback(ConfigFactory.load())
      .resolve()

    val bypass = Configuration.from(config).httpConfiguration.csrfConfig.preAuthVerificationBypass

    assertEquals(bypass.enabled, true)
    assertEquals(bypass.allowPaths, Seq("/auth/verify-token"))
    assertEquals(bypass.allowMethods, Seq("POST"))
    assertEquals(bypass.requiredContentTypes, Seq("application/json"))
    assertEquals(bypass.requiredHeaders, Seq("X-Verify-Channel"))
    assertEquals(bypass.requiredHeaderValues, Map("X-Verify-Channel" -> "internet-client"))
  }

  test("pre-auth verification bypass disabled blocks verify route") {
    val response = doVerifyPost(PreAuthVerificationBypassConfiguration())

    assertEquals(response.statusCode(), 401)
    assert(response.body().contains("<title>Unauthorized Access</title>"))
  }

  test("pre-auth verification bypass exact route and method allows verify route") {
    val response = doVerifyPost(
      PreAuthVerificationBypassConfiguration(
        enabled = true,
        allowPaths = Seq("/auth/verify-token"),
        allowMethods = Seq("POST"),
        requiredContentTypes = Seq("application/json"),
        requiredHeaders = Seq("X-Verify-Channel")
      ),
      additionalHeaders = Seq("X-Verify-Channel" -> "internet-client")
    )

    assertEquals(response.statusCode(), 200)
    assertNoDiff(response.body(), "Verified")
  }

  test("pre-auth verification bypass wrong path stays CSRF protected") {
    val response = doVerifyPost(
      PreAuthVerificationBypassConfiguration(
        enabled = true,
        allowPaths = Seq("/auth/verify-token/other"),
        allowMethods = Seq("POST"),
        requiredContentTypes = Seq("application/json")
      )
    )

    assertEquals(response.statusCode(), 401)
  }

  test("pre-auth verification bypass wrong method stays CSRF protected") {
    val response = doVerifyPost(
      PreAuthVerificationBypassConfiguration(
        enabled = true,
        allowPaths = Seq("/auth/verify-token"),
        allowMethods = Seq("PUT"),
        requiredContentTypes = Seq("application/json")
      )
    )

    assertEquals(response.statusCode(), 401)
  }

  test("pre-auth verification bypass missing required header fails closed") {
    val response = doVerifyPost(
      PreAuthVerificationBypassConfiguration(
        enabled = true,
        allowPaths = Seq("/auth/verify-token"),
        allowMethods = Seq("POST"),
        requiredContentTypes = Seq("application/json"),
        requiredHeaders = Seq("X-Verify-Channel")
      )
    )

    assertEquals(response.statusCode(), 401)
  }

  test("pre-auth verification bypass content-type mismatch fails closed") {
    val response = doVerifyPost(
      PreAuthVerificationBypassConfiguration(
        enabled = true,
        allowPaths = Seq("/auth/verify-token"),
        allowMethods = Seq("POST"),
        requiredContentTypes = Seq("application/json")
      ),
      contentType = MediaType.PLAIN_TEXT.toString
    )

    assertEquals(response.statusCode(), 401)
  }

  test("pre-auth verification bypass with requiredHeaderValues value match succeeds") {
    val response = doVerifyPost(
      PreAuthVerificationBypassConfiguration(
        enabled = true,
        allowPaths = Seq("/auth/verify-token"),
        allowMethods = Seq("POST"),
        requiredContentTypes = Seq("application/json"),
        requiredHeaders = Seq("X-Verify-Channel"),
        requiredHeaderValues = Map("X-Verify-Channel" -> "internet-client")
      ),
      additionalHeaders = Seq("X-Verify-Channel" -> "internet-client")
    )

    assertEquals(response.statusCode(), 200)
    assertNoDiff(response.body(), "Verified")
  }

  test("pre-auth verification bypass with requiredHeaderValues mismatch fails closed") {
    val response = doVerifyPost(
      PreAuthVerificationBypassConfiguration(
        enabled = true,
        allowPaths = Seq("/auth/verify-token"),
        allowMethods = Seq("POST"),
        requiredContentTypes = Seq("application/json"),
        requiredHeaders = Seq("X-Verify-Channel"),
        requiredHeaderValues = Map("X-Verify-Channel" -> "expected-value")
      ),
      additionalHeaders = Seq("X-Verify-Channel" -> "wrong-value")
    )

    assertEquals(response.statusCode(), 401)
  }

  test("pre-auth verification bypass trailing slash in config path normalization") {
    val response = doVerifyPost(
      PreAuthVerificationBypassConfiguration(
        enabled = true,
        allowPaths = Seq("/auth/verify-token/"), // config has trailing slash
        allowMethods = Seq("POST"),
        requiredContentTypes = Seq("application/json"),
        requiredHeaders = Seq("X-Verify-Channel")
      ),
      additionalHeaders = Seq("X-Verify-Channel" -> "internet-client")
    )

    assertEquals(response.statusCode(), 200)
    assertNoDiff(response.body(), "Verified")
  }

  test("pre-auth verification bypass case-sensitive path does not match different case") {
    val response = doVerifyPost(
      PreAuthVerificationBypassConfiguration(
        enabled = true,
        allowPaths = Seq("/auth/verify-token"),
        allowMethods = Seq("POST"),
        requiredContentTypes = Seq("application/json")
      ),
      requestPath = "/Auth/Verify-Token"
    )

    assertEquals(response.statusCode(), 401)
  }

  test("pre-auth verification bypass with minimal config (path + method only) succeeds") {
    val response = doVerifyPost(
      PreAuthVerificationBypassConfiguration(
        enabled = true,
        allowPaths = Seq("/auth/verify-token"),
        allowMethods = Seq("POST")
      )
    )

    assertEquals(response.statusCode(), 200)
    assertNoDiff(response.body(), "Verified")
  }

  private def doVerifyPost(
    bypassConfig: PreAuthVerificationBypassConfiguration,
    contentType: String = MediaType.JSON.toString,
    additionalHeaders: Seq[(String, String)] = Nil,
    requestPath: String = "/auth/verify-token"
  )(using loc: munit.Location): http.HttpResponse[String] =
    val server = Server(0)
      .setPreAuthVerificationBypass(bypassConfig)
      .addServices(CSRFServices)
      .addCSRFGuard((_ /*origin*/, _ /*referer*/, _ /*ctx*/) => false)
      .start()

    try
      val target = s"http://localhost:${server.port}"
      val requestBuilder = http.HttpRequest
        .newBuilder(URI.create(s"$target$requestPath"))
        .timeout(Duration.ofSeconds(30))
        .POST(http.HttpRequest.BodyPublishers.ofString("""{"token":"abc"}"""))
        .header("Content-Type", contentType)

      additionalHeaders.foreach { case (name, value) =>
        requestBuilder.header(name, value)
      }

      http.HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()
        .send(requestBuilder.build(), http.HttpResponse.BodyHandlers.ofString())
    finally
      server.stop()


