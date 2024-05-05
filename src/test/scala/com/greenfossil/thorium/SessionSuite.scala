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

import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.server.annotation.Get

import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.{CookieManager, URI}

object SessionServices {
  @Get("/s0")
  def s0 = Action { request =>
    Redirect("/s1", HttpStatus.SEE_OTHER).withNewSession.flashing("News Flash" ->"Flash Value")
  }

  @Get("/s1")
  def s1 = Action { request =>
    Redirect("/s2").withSession(request.session + ("foo" -> "bar"))
  }

  @Get("/s2")
  def s2 = Action {request =>
    Redirect("/s3").withSession(request.session + ("baz" -> "foobaz"))
  }

  @Get("/s3")
  def s3 = Action {request =>
    Ok(s"S3 reached ${request.session}").withSession(request.session + ("baz" -> "foobaz"))
  }
}

class SessionSuite extends munit.FunSuite {

  test("Session, Flash propagation") {
    val server = Server(0)
      .addServices(SessionServices)
      .start()

    val cm = CookieManager()
    val req = HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/s0")).build()
    val resp = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .cookieHandler(cm)
      .build()
      .send(req, HttpResponse.BodyHandlers.ofString())
    assert(resp.body().startsWith("S3 reached"))
    assertEquals(cm.getCookieStore.getCookies.size(), 1)
    assertNoDiff(cm.getCookieStore.getCookies.stream().filter(_.getName == "APP_SESSION").findFirst().get.getName, "APP_SESSION")
    server.stop()
  }

}
