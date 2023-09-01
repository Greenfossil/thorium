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

import com.linecorp.armeria.common.{Cookie, HttpStatus}
import com.linecorp.armeria.server.annotation.Get

import scala.annotation.nowarn

object HeadersServices {
  @Get("/headers") @nowarn
  def headers = Action { request =>
    Ok("Headers sent")
      .withHeaders(
        "Access-Control-Allow-Origin" -> "*",
        "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept")
      .withSession("session" -> "sessionValue")
      .flashing("flash" -> "flashValue")
      .withCookies(Cookie.of("Cookie1", "Cookie1Value"), Cookie.of("Cookie2", "CookieValue2"))
  }

}

class HeadersSuite extends munit.FunSuite {
  test("header, session, flash"){
    val server = Server()
      .addServices(HeadersServices)
      .start()

    import com.linecorp.armeria.client.WebClient
    val client = WebClient.of(s"http://localhost:${server.port}")
    client.get("/headers").aggregate().thenApply{ aggResp =>
      assertEquals(aggResp.status(), HttpStatus.OK)
      assertNoDiff(aggResp.contentUtf8(), "Headers sent")
    }.join()
    server.stop()
  }

}
