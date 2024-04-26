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

import com.linecorp.armeria.server.annotation.{Get, Param, Post}

import java.net.{URI, http}
import scala.language.implicitConversions

private object Service:

  @Get("/foo")
  def foo(@Param msg:String)(req: Request) =
    s"${req.path} content:$msg"

  @Get("/bar")
  def bar(@Param msg:String)(implicit req: Request) =
     s"${req.path} content:$msg"

  @Get("/baz/:name")
  def baz(@Param name: String)(implicit req: Request) =
   s"${req.path} name:$name"

  @Post("/foobaz/:name")
  def foobaz(form: FormUrlEndcoded)(implicit req: Request) =
    s"${req.path} form:${form} content:${req.asText}"

end Service

class AddAnnotatedServiceSuite extends munit.FunSuite:
  import com.linecorp.armeria.common.*

  var server: Server = null

  override def beforeAll(): Unit = {
    server = Server(0)
      .addServices(Service)
      .start()
  }

  override def afterAll(): Unit = {
    Thread.sleep(1000)
    server.stop()
  }

  test("foo") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/foo?msg=HelloWorld!"))
        .header("Content-Type", MediaType.PLAIN_TEXT.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertNoDiff(resp.body(), "/foo content:HelloWorld!")
  }

  test("bar") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/bar?msg=HelloWorld!"))
        .header("Content-Type", MediaType.PLAIN_TEXT.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertNoDiff(resp.body(), "/bar content:HelloWorld!")
  }

  test("baz") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/baz/homer"))
        .header("Content-Type", MediaType.PLAIN_TEXT.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertNoDiff(resp.body(), "/baz/homer name:homer")
  }

  test("foobaz") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/foobaz/homer"))
        .POST(http.HttpRequest.BodyPublishers.ofString("msg[]=Hello&msg[]=World!"))
        .header("Content-Type", MediaType.FORM_DATA.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertNoDiff(resp.body(), "/foobaz/homer form:FormUrlEndcoded(ListMap(msg[] -> List(Hello, World!))) content:msg[]=Hello&msg[]=World!")
  }

  test("foobaz - wrong media-type") {
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/foobaz/homer"))
        .POST(http.HttpRequest.BodyPublishers.ofString("msg[]=Hello&msg[]=World!"))
        .header("Content-Type", MediaType.JSON.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), HttpStatus.BAD_REQUEST.code())
  }
