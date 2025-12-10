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
import scala.sys.process.*

import java.net.{URI, http}
import scala.language.implicitConversions

private object SensitiveDataService:

  @Post("/form")
  def form(implicit req: Request) =
    s"${req.path} content:${req.asText}"

  @Post("/json")
  def json(implicit req: Request) =
    s"${req.path} content:${req.asText}"


  @Post("/multipart")
  def multipart: Action = Action.multipart { implicit request =>
    s"${request.path} multipart received"
  }

end SensitiveDataService

class FirstResponderSuite extends munit.FunSuite:
  import com.linecorp.armeria.common.*

  var server: Server = null

  override def beforeAll(): Unit = {
    server = Server(0)
      .addServices(SensitiveDataService)
      .start()
  }

  override def afterAll(): Unit = {
    Thread.sleep(1000)
    server.stop()
  }

  test("form-url-encoded with sensitive data") {
    val fields = List(
      "username" -> "homer",
      "password" -> "secret",
      "email" -> "johndoe@example.com",
      "phone" -> "1234567890",
      "mobile" -> "0987654321",
      "creditcard" -> "4111111111111111",
    )
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/form"))
        .POST(http.HttpRequest.BodyPublishers.ofString(fields.map{case(k,v) => s"$k=$v"}.mkString("&")))
        .header("Content-Type", MediaType.FORM_DATA.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200)
  }

  test("json with sensitive data") {
    val jsonBody = """{"username": "john", "password": "secret123", "api_key": "xyz789"}"""
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/json"))
        .POST(http.HttpRequest.BodyPublishers.ofString(jsonBody))
        .header("Content-Type", MediaType.JSON.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200)
  }

  test("plain text with sensitive data") {
    val jsonBody = """{"username": "john", "password": "secret123", "api_key": "xyz789"}"""
    val resp = http.HttpClient.newHttpClient().send(
      http.HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/json"))
        .POST(http.HttpRequest.BodyPublishers.ofString(jsonBody))
        .header("Content-Type", MediaType.PLAIN_TEXT.toString)
        .build(),
      http.HttpResponse.BodyHandlers.ofString()
    )
    assertEquals(resp.statusCode(), 200)
  }

  test("multipart POST without file content but with form param") {
    val result = s"curl http://localhost:${server.port}/multipart -F name=homer -F password=secret123 -F mobile=0987654321".!!.trim
    assertEquals(result, "/multipart multipart received")
  }
