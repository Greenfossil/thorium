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

import com.linecorp.armeria.client.logging.LoggingClient
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.server.annotation.Get

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
    println(s"S2 request.session = ${request.session}, flash ${request.flash}") //foo->bar
    Redirect("/s3").withSession(request.session + ("baz" -> "foobaz"))
  }

  @Get("/s3")
  def s3 = Action {request =>
    println(s"S3 request.session = ${request.session}, flash ${request.flash}") //foo->bar, "baz" -> foobaz
    Ok(s"S3 reached ${request.session}").withSession(request.session + ("baz" -> "foobaz"))
  }
}

class SessionSuite extends munit.FunSuite {

  test("Session, Flash propagation") {
    val server = Server()
      .addServices(SessionServices)
      .start()

    import com.linecorp.armeria.client.WebClient
    import com.linecorp.armeria.client.cookie.*
    val client = WebClient
      .builder(s"http://localhost:${server.port}")
      .followRedirects()
      .decorator(CookieClient.newDecorator(CookiePolicy.acceptAll()))
      .decorator(LoggingClient.newDecorator())
      .build()
    val resp = client.get("/s0").aggregate().join()
    assert(resp.contentUtf8().startsWith("S3 reached"))
    server.stop()
  }

}
