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
import com.linecorp.armeria.common.*

import java.net.{CookieManager, HttpCookie, URI, URLEncoder, http}
import java.time.Duration

class CSRFGuardModule_Post_FormData_Pass_Suite extends munit.FunSuite:

  //Important - allow content-length to be sent in the headers
  System.setProperty("jdk.httpclient.allowRestrictedHeaders", "content-length")

  test("SameOrigin POST /csrf/email/change"):
    doPost(identity)


  test("Null Origin POST /csrf/email/change"):
    doPost(_ => null)


  test("CrossOrigin POST /csrf/email/change"):
    doPost(_ => "http://another-site")

  private def doPost(originFn: String => String)(using loc:munit.Location) =
    val server = Server(0)
      .addServices(CSRFServices)
      .addCSRFGuard()
      .start()
    
    val postEpPath = "/csrf/email/change"
    val csrfCookieTokenName = Configuration().httpConfiguration.csrfConfig.cookieName
    val target = s"http://localhost:${server.port}"
    val csrfCookie: Cookie = CSRFGuardModule.generateCSRFTokenCookie(Configuration(), Some("ABC"))
    val content = s"email=password&${csrfCookieTokenName}=${URLEncoder.encode(csrfCookie.value(), "UTF-8")}"
    
    val cm = CookieManager()
    val cookie = HttpCookie(csrfCookieTokenName, csrfCookie.value())
    cookie.setDomain(csrfCookie.domain())
    cookie.setPath(csrfCookie.path())
    cm.getCookieStore.add(URI.create(target), cookie)

    //Set up Request
    val requestBuilder = http.HttpRequest.newBuilder(URI.create(target + postEpPath))
      .POST(http.HttpRequest.BodyPublishers.ofString(content))
      .headers(
        "content-type", MediaType.FORM_DATA.toString,
        "content-length", content.length.toString //required to set allowRestrictedHeaders
      )

    //Set origin
    Option(originFn(target)).foreach(target => requestBuilder.header("Origin", target))

    val postResp = http.HttpClient.newBuilder().cookieHandler(cm).connectTimeout(Duration.ofHours(1)).build()
      .send(requestBuilder.build(), http.HttpResponse.BodyHandlers.ofString())
    assertEquals(postResp.statusCode(), 200)
    assertNoDiff(postResp.body(), "Password Changed")


    server.stop()