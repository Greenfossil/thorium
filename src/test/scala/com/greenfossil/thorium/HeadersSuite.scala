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

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{Cookie, HttpStatus}
import com.linecorp.armeria.server.annotation.Get

import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.{CookieManager, HttpCookie, URI}
import java.util.Base64
import scala.annotation.nowarn

object HeadersServices:
  @Get("/headers") @nowarn
  def headers = Action { implicit request =>
    Ok("Headers sent")
      .withHeaders(
        "Access-Control-Allow-Origin" -> "*",
        "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept")
      .withSession("session" -> "sessionValue")
      .flashing("flash" -> "flashValue")
      .withCookies(Cookie.ofSecure("Cookie1", "Cookie1Value"), Cookie.ofSecure("Cookie2", "Cookie2Value"))
      .withCookies(CookieUtil.bakeCookies("json-cookie", Json.obj("user" -> "homer"))*)
  }
end HeadersServices

class HeadersSuite extends munit.FunSuite {
  test("header, session, flash"){
    val server = Server(0)
      .addServices(HeadersServices)
      .start()

    val cm = CookieManager()
    val resp = HttpClient.newBuilder().cookieHandler(cm).build().send(
      HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/headers")).build(),
      HttpResponse.BodyHandlers.ofString()
    )
    def findCookie(name: String): HttpCookie =
      val opt = cm.getCookieStore.getCookies.stream().filter(_.getName == name).findFirst()
      opt.orElseGet(() => null)

    assertNoDiff(resp.headers().firstValue("access-control-allow-origin").get, "*")
    assertNoDiff(resp.headers().firstValue("access-control-allow-headers").get, "Origin, X-Requested-With, Content-Type, Accept")
    assertEquals(resp.statusCode(), HttpStatus.OK.code())
    assertEquals(findCookie("APP_SESSION").getSecure, false)
    assertEquals(findCookie("APP_FLASH").getSecure, false)
    assertEquals(findCookie("Cookie1").getValue, "Cookie1Value")
    assertEquals(findCookie("Cookie2").getValue, "Cookie2Value")
    assertNoDiff(new String(Base64.getDecoder.decode(findCookie("json-cookie").getValue)), """{"user":"homer"}""")
    assertNoDiff(resp.body(), "Headers sent")
    server.stop()
  }

}
