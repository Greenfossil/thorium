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
import io.github.yskszk63.jnhttpmultipartformdatabodypublisher.MultipartFormDataBodyPublisher

import java.io.ByteArrayInputStream
import java.net.{CookieManager, URI, http}

class CSRFGuardModule_Post_Multipart_Pass_Suite extends munit.FunSuite:

  test("SameOrigin POST"):
    doPost(identity)

  test("null Origin POST"):
    doPost(_ => null)

  test("Cross Origin POST"):
    doPost(_ => "http://another-site")

  def doPost(originFn: String => String)(using loc: munit.Location) =
    val server = Server(0)
      .addServices(CSRFServices)
      .addCSRFGuard()
      .start()
    val port = server.port

    val postEpPath = "/csrf/multipart-file"
    val target = s"http://localhost:$port"
    val csrfCookieTokenName = Configuration().httpConfiguration.csrfConfig.cookieName

    //Get the CSRFToken cookie
    val csrfCookie = CSRFGuardModule.generateCSRFTokenCookie(Configuration(), Some("ABC"))

    val cm = CookieManager()
    cm.getCookieStore.add(target, csrfCookie)
    val mpPub = MultipartFormDataBodyPublisher()
      .add("name", "Homer")
      .add(csrfCookieTokenName, csrfCookie.value())
      .addStream("file", "file.txt", () => ByteArrayInputStream("Hello world".getBytes), "text/plain")

    val reqBuilder = http.HttpRequest.newBuilder(URI.create(s"$target$postEpPath"))
      .POST(mpPub)
      .header("Content-Type", mpPub.contentType())

    //Set Origin
    Option(originFn(target)).foreach(target => reqBuilder.header("Origin", target))

    val resp = http.HttpClient.newBuilder().cookieHandler(cm).build()
      .send(
        reqBuilder
          .POST(mpPub)
          .build(),
        http.HttpResponse.BodyHandlers.ofString()
      )
    assertEquals(resp.statusCode(), 200)
    assertNoDiff(resp.body(), "Received multipart request with files: 1")

    server.stop()
