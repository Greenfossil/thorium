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

import com.greenfossil.thorium.decorators.CSRFProtectionDecoratingFunction
import com.linecorp.armeria.client.*
import com.linecorp.armeria.common.*
import com.linecorp.armeria.common.multipart.{BodyPart, Multipart}
import com.linecorp.armeria.common.stream.StreamMessage

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Duration

class CSRFProtectionDecoratingFunction3Suite extends munit.FunSuite:

  test("SameOrigin POST"):
    doPost(identity)

  test("null Origin POST"):
    doPost(_ => null)

  test("Cross Origin POST"):
    doPost(_ => "http://another-site")

  def doPost(originFn: String => String)(using loc: munit.Location) =
    val server = Server(0)
      .addServices(CSRFServices)
      .addCSRFProtection()
      .start()
    val port = server.port

    val postEpPath = "/csrf/multipart-file"
    val csrfCookieTokenName = Configuration().httpConfiguration.csrfConfig.cookieName
    val target = s"http://localhost:${port}"
    val client = WebClient.of(target)

    //Get the CSRFToken cookie
    val csrfCookie = CSRFProtectionDecoratingFunction.generateCSRFTokenCookie(Configuration(), Some("ABC"))

    //Create 3 parts, formPart, csrfPart and filePart
    val formPart = BodyPart.of(ContentDisposition.of("form-data", "name"), "Homer")
    val csrfPart = BodyPart.of(ContentDisposition.of("form-data", csrfCookieTokenName), csrfCookie.value())
    val filePath = Files.write(Paths.get("/tmp/file.txt"), "Hello world".getBytes(StandardCharsets.UTF_8))
    val filePart = BodyPart.of(ContentDisposition.of("form-data", "file", "file.txt"), StreamMessage.of(filePath))

    //Create multipart request
    val multipart = Multipart.of(csrfPart, formPart, filePart)
    val multipartRequest = multipart.toHttpRequest(postEpPath)

    //Set Origin
    val csrfMultipartRequest =
      multipartRequest.mapHeaders{headers =>
        val headersBuilder = headers.toBuilder
        headersBuilder.cookies(csrfCookie)
        Option(originFn(target)).map(target => headersBuilder.set("Origin", target))
        headersBuilder.build()
      }

    //Set response time
    val reqOpts = RequestOptions.builder().responseTimeout(Duration.ofHours(1)).build()
    val postResp = client.execute(csrfMultipartRequest, reqOpts).aggregate().join()
    assertNoDiff(postResp.status().codeAsText(), "200")
    assertNoDiff(postResp.contentUtf8(), s"Received multipart request with files: 1, form:FormUrlEndcoded(Map(file -> List(file.txt), name -> List(Homer), APP_CSRF_TOKEN -> List(${csrfCookie.value()})))")

    server.stop()
