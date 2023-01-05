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

import com.linecorp.armeria.server.annotation.{Get, Param}

object TestServices:
  @Get("/sayHello/:name")
  def sayHello(@Param name: String)(using request: Request): String =
    s"Hello $name"

  @Get("/redirect")
  def redirect(using request: Request): Result =
    Redirect(sayHello("User"))

  @Get("/path")
  def path(using request: Request): Result =
    val url = request.refererOpt.getOrElse("/redirect")
    Redirect(url)

class EndpointMacro3Suite extends munit.FunSuite:

  test("redirect") {
    val server = Server()
      .addServices(TestServices)
      .start()

    import com.linecorp.armeria.client.WebClient
    val client = WebClient.of(s"http://localhost:${server.port}")
    val resp = client.get("/redirect")
    resp.aggregate().thenApply { aggResp =>
      val locationHeader = aggResp.headers().get("location")
      assertEquals(locationHeader, "/sayHello/User")
    }.join()
    server.stop()
  }

  test("path") {
    val server = Server()
      .addServices(TestServices)
      .start()

    import com.linecorp.armeria.client.WebClient
    val client = WebClient.of(s"http://localhost:${server.port}")
    val resp = client.get("/path")
    resp.aggregate().thenApply { aggResp =>
      val locationHeader = aggResp.headers().get("location")
      assertEquals(locationHeader, "/redirect")
    }.join()
    server.stop()
  }
