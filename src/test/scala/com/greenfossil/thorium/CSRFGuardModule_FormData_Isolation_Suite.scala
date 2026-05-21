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

import com.greenfossil.thorium.decorators.CSRFGuardModule
import com.linecorp.armeria.common.{Cookie, MediaType}

import java.net.{CookieManager, HttpCookie, URI, URLEncoder, http}
import java.time.Duration

class CSRFGuardModule_FormData_Isolation_Suite extends munit.FunSuite:

  System.setProperty("jdk.httpclient.allowRestrictedHeaders", "content-length")

  private val bypassConfig = PreAuthVerificationBypassConfiguration(
    enabled = true,
    allowPaths = Seq("/auth/verify-token"),
    allowMethods = Seq("POST"),
    requiredContentTypes = Seq("application/json"),
    requiredHeaders = Seq("X-Verify-Channel")
  )

  test("form-data mutating route remains CSRF-blocked when bypass config targets another path") {
    val response = doFormPost(includeValidCsrf = false, origin = "http://another-site")

    assertEquals(response.statusCode(), 401)
  }

  test("form-data mutating route still succeeds with valid CSRF token pair when bypass config is enabled") {
    val response = doFormPost(includeValidCsrf = true, origin = "http://another-site")

    assertEquals(response.statusCode(), 200)
    assertNoDiff(response.body(), "Password Changed")
  }

  private def doFormPost(includeValidCsrf: Boolean, origin: String)(using loc: munit.Location): http.HttpResponse[String] =
    val baseConfiguration = Configuration()
    val server = Server(0)
      .setPreAuthVerificationBypass(bypassConfig)
      .addServices(CSRFServices)
      .addCSRFGuard()
      .start()

    try
      val target = s"http://localhost:${server.port}"
      val postEpPath = "/csrf/email/change"
      val csrfCookieTokenName = baseConfiguration.httpConfiguration.csrfConfig.cookieName
      val cookieManager = CookieManager()

      val content =
        if includeValidCsrf then createValidCsrfFormContent(baseConfiguration, csrfCookieTokenName, cookieManager, target)
        else "email=password"

      val requestBuilder = http.HttpRequest
        .newBuilder(URI.create(target + postEpPath))
        .timeout(Duration.ofSeconds(30))
        .POST(http.HttpRequest.BodyPublishers.ofString(content))
        .headers(
          "content-type", MediaType.FORM_DATA.toString,
          "content-length", content.length.toString,
          "Origin", origin
        )

      http.HttpClient
        .newBuilder()
        .cookieHandler(cookieManager)
        .connectTimeout(Duration.ofSeconds(30))
        .build()
        .send(requestBuilder.build(), http.HttpResponse.BodyHandlers.ofString())
    finally
      server.stop()

  private def createValidCsrfFormContent(
    baseConfiguration: Configuration,
    csrfCookieTokenName: String,
    cookieManager: CookieManager,
    target: String
  ): String =
    val csrfCookie: Cookie = CSRFGuardModule.generateCSRFTokenCookie(baseConfiguration, Some("ABC"))
    val cookie = HttpCookie(csrfCookieTokenName, csrfCookie.value())
    cookie.setDomain(csrfCookie.domain())
    cookie.setPath(csrfCookie.path())
    cookieManager.getCookieStore.add(URI.create(target), cookie)
    s"email=password&${csrfCookieTokenName}=${URLEncoder.encode(csrfCookie.value(), "UTF-8")}"


