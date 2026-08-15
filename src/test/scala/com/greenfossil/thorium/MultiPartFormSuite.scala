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

import com.linecorp.armeria.common.{HttpStatus, MediaType}
import com.linecorp.armeria.server.annotation.Post
import io.github.yskszk63.jnhttpmultipartformdatabodypublisher.MultipartFormDataBodyPublisher
import munit.FunSuite

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

object FormServices {
  @Post("/multipart3")
  def multipartForm3: Action = Action.multipart { implicit request =>
    request.findFiles((_, _, _, _) => true)
      .map{files =>
        Ok(s"Received multipart request with files: ${files.size}")
      }.getOrElse(
        BadRequest(s"Invalid multipart request")
      )
  }

  @Post("/multipart-bad-request")
  def multipartFormBadRequest: Action = Action.multipart { implicit request =>
    BadRequest("Bad Request")
  }

  @Post("/multipart-bad-request-2")
  def multipartForm4: Action = Action.multipart { implicit request =>
    "Bad Request 2".as(HttpStatus.BAD_REQUEST, MediaType.JSON)
  }

  @Post("/multipart-internal-server-error")
  def multipartFormInternalServerError: Action = Action.multipart { implicit request =>
    InternalServerError("Internal Server Error")
  }

}

class MultiPartFormSuite extends FunSuite{

  var server: Server = null
  val client = HttpClient.newHttpClient()

  override def beforeAll(): Unit =
    server = Server(0)
      .addServices(FormServices)
      .start()

  override def afterAll(): Unit =
    if server != null then server.stop()

  private def postMultipart(path: String, mpPub: MultipartFormDataBodyPublisher): HttpResponse[String] =
    client.send(
      HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}$path"))
        .POST(mpPub)
        .header("Content-Type", mpPub.contentType())
        .build(),
      HttpResponse.BodyHandlers.ofString()
    )

  private def postRawMultipart(path: String, boundary: String, body: String): HttpResponse[String] =
    val bytes = body.getBytes(StandardCharsets.UTF_8)
    client.send(
      HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}$path"))
        .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
        .header("Content-Type", s"multipart/form-data; boundary=$boundary")
        .build(),
      HttpResponse.BodyHandlers.ofString()
    )

  test("POST with file content") {
    Files.write(Paths.get("/tmp/file.txt"), "Hello world".getBytes(StandardCharsets.UTF_8))
    val mpPub = MultipartFormDataBodyPublisher()
      .addFile("resourceFile", Paths.get("/tmp/file.txt"), "text/plain")
    val resp = postMultipart("/multipart3", mpPub)
    assertEquals(resp.statusCode(), 200)
    assertNoDiff(resp.body(), "Received multipart request with files: 1")
  }

  test("POST without file content but with form param") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val resp = postMultipart("/multipart3", mpPub)
    assertEquals(resp.statusCode(), 200)
    assertNoDiff(resp.body(), "Received multipart request with files: 0")
  }

  test("POST empty body raw data") {
    val boundary = "----WebKitFormBoundaryaDaB4MtEkj4a1pYx"
    val body = s"------$boundary--\r\n"
    val resp = postRawMultipart("/multipart3", boundary, body)
    assertEquals(resp.statusCode(), 200)
    assertNoDiff(resp.body(), "Received multipart request with files: 0")
  }

  test("POST without file content but with form param, using web browser's raw data") {
    val boundary = "----WebKitFormBoundaryOrtvYGBXE2gxan8t"
    val body =
      s"--$boundary\r\nContent-Disposition: form-data; name=\"url\"\r\n\r\n\r\n" +
      s"--$boundary\r\nContent-Disposition: form-data; name=\"status\"\r\n\r\nactive\r\n" +
      s"--$boundary\r\nContent-Disposition: form-data; name=\"description\"\r\n\r\n\r\n" +
      s"--$boundary\r\nContent-Disposition: form-data; name=\"tpe\"\r\n\r\n\r\n" +
      s"--$boundary\r\nContent-Disposition: form-data; name=\"spaceId\"\r\n\r\n\r\n" +
      s"--$boundary\r\nContent-Disposition: form-data; name=\"roleFilter\"\r\n\r\non\r\n" +
      s"--$boundary\r\nContent-Disposition: form-data; name=\"dtStart\"\r\n\r\n\r\n" +
      s"--$boundary\r\nContent-Disposition: form-data; name=\"dtEnd\"\r\n\r\n\r\n" +
      s"--$boundary--\r\n"
    val resp = postRawMultipart("/multipart3", boundary, body)
    assertEquals(resp.statusCode(), 200)
    assertNoDiff(resp.body(), "Received multipart request with files: 0")
  }

  test("POST with bad request") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val resp = postMultipart("/multipart-bad-request", mpPub)
    assertEquals(resp.statusCode(), HttpStatus.BAD_REQUEST.code())
    assertNoDiff(resp.body(), "Bad Request")
  }

  test("POST with bad request 2") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val resp = postMultipart("/multipart-bad-request-2", mpPub)
    assertEquals(resp.statusCode(), HttpStatus.BAD_REQUEST.code())
    assertNoDiff(resp.body(), "Bad Request 2")
  }

  test("POST with internal server error") {
    val mpPub = MultipartFormDataBodyPublisher().add("name", "homer")
    val resp = postMultipart("/multipart-internal-server-error", mpPub)
    assertEquals(resp.statusCode(), HttpStatus.INTERNAL_SERVER_ERROR.code())
    assertNoDiff(resp.body(), "Internal Server Error")
  }

}