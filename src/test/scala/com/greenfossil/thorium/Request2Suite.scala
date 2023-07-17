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

import scala.language.implicitConversions

private object Service:

  @Get("/foo")
  def foo(req: Request) =
    s"${req.path} content:${req.asText}"

  @Get("/bar")
  def bar(implicit req: Request) =
     s"${req.path} content:${req.asText}"

  @Get("/baz/:name")
  def baz(@Param name: String)(implicit req: Request) =
   s"${req.path} name:${name} content:${req.asText}"

  @Post("/foobaz/:name")
  def foobaz(form: FormUrlEndcoded)(implicit req: Request) =
    s"${req.path} form:${form} content:${req.asText}"

class Request2Suite extends munit.FunSuite:
  import com.linecorp.armeria.client.*
  import com.linecorp.armeria.common.*

  var server: Server = null

  override def beforeAll(): Unit = {
    server = Server()
      .addServices(Service)
      .start()
  }

  override def afterAll(): Unit = {
    Thread.sleep(1000)
    server.stop()
  }

  import com.linecorp.armeria.scala.implicits.*

  test("foo") {
    val client = WebClient.of(s"http://localhost:${server.port}")

    val creq = HttpRequest.of(HttpMethod.GET, "/foo", MediaType.PLAIN_TEXT, "HelloWorld!")
    client.execute(creq).aggregate().thenAccept(
      aggregate =>
        assertNoDiff(aggregate.contentUtf8(), "/foo content:HelloWorld!")
    ).toScala
  }

  test("bar") {
    val client = WebClient.of(s"http://localhost:${server.port}")

    val creq = HttpRequest.of(HttpMethod.GET, "/bar", MediaType.PLAIN_TEXT, "HelloWorld!")
    client.execute(creq).aggregate().thenAccept(
      aggregate =>
        assertNoDiff(aggregate.contentUtf8(), "/bar content:HelloWorld!")
    ).toScala
  }

  test("baz") {
    val client = WebClient.of(s"http://localhost:${server.port}")

    val creq = HttpRequest.of(HttpMethod.GET, "/baz/homer", MediaType.PLAIN_TEXT, "HelloWorld!")
    client.execute(creq).aggregate().thenAccept(
      aggregate =>
        assertNoDiff(aggregate.contentUtf8(), "/baz/homer name:homer content:HelloWorld!")
    ).toScala
  }

  test("foobaz") {
    val client = WebClient.of(s"http://localhost:${server.port}")

    val creq = HttpRequest.of(HttpMethod.POST, "/foobaz/homer", MediaType.FORM_DATA, "msg[]=Hello&msg[]=World!")
    client.execute(creq).aggregate().thenAccept(
      aggregate =>
        assertNoDiff(aggregate.contentUtf8(), "/foobaz/homer form:FormUrlEndcoded(ListMap(msg[] -> List(Hello, World!))) content:msg[]=Hello&msg[]=World!")
    ).toScala
  }

  test("foobaz - wrong media-type") {
    val client = WebClient.of(s"http://localhost:${server.port}")

    val creq = HttpRequest.of(HttpMethod.POST, "/foobaz/homer", MediaType.JSON, "msg[]=Hello&msg[]=World!")
    client.execute(creq).aggregate().thenAccept(
      aggregate =>
        assertEquals(aggregate.status(), HttpStatus.BAD_REQUEST)
    ).toScala
  }

