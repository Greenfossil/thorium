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
import munit.IgnoreSuite

import java.net.{URI, http}
import java.time.Duration

@IgnoreSuite
class CSRFGuardModule_Post_FormData_Fail_Suite extends munit.FunSuite:

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
      .addCSRFGuard((_ /*origin*/, _ /*referer*/, _ /*ctx*/) => false)
      .start()
    
    val postEpPath = "/csrf/email/change"
    val target = s"http://localhost:${server.port}"
    val content = s"email=password"
    
    //Set up Request
    val requestBuilder = http.HttpRequest.newBuilder(URI.create(target + postEpPath))
      .POST(http.HttpRequest.BodyPublishers.ofString(content))
      .headers(
        "content-type", MediaType.FORM_DATA.toString,
        "content-length", content.length.toString //required to set allowRestrictedHeaders
      )

    //Set origin
    Option(originFn(target)).foreach(target => requestBuilder.header("Origin", target))
    
    val postResp = http.HttpClient.newBuilder().connectTimeout(Duration.ofHours(1)).build()
      .send(requestBuilder.build(), http.HttpResponse.BodyHandlers.ofString())

    assertEquals(postResp.statusCode(), 401)
    assertNoDiff(postResp.body(), """|<!DOCTYPE html>
                                            |<html>
                                            |<head>
                                            |  <title>Unauthorized Access</title>
                                            |</head>
                                            |<body>
                                            |  <h1>Access Denied</h1>
                                            |</body>
                                            |</html>
                                            |""".stripMargin)
    
    server.stop()