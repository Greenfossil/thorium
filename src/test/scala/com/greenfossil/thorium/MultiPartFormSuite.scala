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

import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.common.{ContentDisposition, HttpStatus, MediaType}
import com.linecorp.armeria.common.multipart.{BodyPart, Multipart}
import com.linecorp.armeria.server.annotation.Post
import munit.FunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.sys.process.*

/*
 * Please ensure com.greenfossil.webserver.examples.main is started
 */
object FormServices {
  @Post("/multipart3")
  def multipartForm3: Action = Action.multipart { implicit request =>
    val files = request.files
    Ok(s"Received multipart request with files: ${files.size}")
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

  override def beforeAll(): Unit =
    try{
      server = Server(0)
        .addServices(FormServices)
        .start()
    }catch {
      case ex: Throwable =>
    }

  override def afterAll(): Unit =
    server.stop()

  test("POST with file content") {
    Files.write(Paths.get("/tmp/file.txt"), "Hello world".getBytes(StandardCharsets.UTF_8))

    val result = s"curl http://localhost:${server.port}/multipart3 -F resourceFile=@/tmp/file.txt ".!!.trim
    assertEquals(result, "Received multipart request with files: 1")
  }

  test("POST without file content but with form param") {
    val result = s"curl http://localhost:${server.port}/multipart3 -F name=homer ".!!.trim
    assertEquals(result, "Received multipart request with files: 0")
  }

  test("POST empty body raw data") {
    val result =
      (s"curl http://localhost:${server.port}/multipart3 -H 'Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryaDaB4MtEkj4a1pYx' " +
        "--data-raw $'------WebKitFormBoundaryaDaB4MtEkj4a1pYx--\r\n'").!!.trim
    assertEquals(result, "Received multipart request with files: 0")
  }

  test("POST without file content but with form param, using web browser's raw data") {
    val command = s"curl http://localhost:${server.port}/multipart3 -H 'Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryOrtvYGBXE2gxan8t' " +
      "--data-raw $'------WebKitFormBoundaryOrtvYGBXE2gxan8t\r\nContent-Disposition: form-data; name=\"resourceFile\"; " +
      "filename=\"\"\r\nContent-Type: application/octet-stream\r\n\r\n\r\n" +
      "------WebKitFormBoundaryOrtvYGBXE2gxan8t\r\nContent-Disposition: form-data; name=\"url\"\r\n\r\n\r\n" +
      "------WebKitFormBoundaryOrtvYGBXE2gxan8t\r\nContent-Disposition: form-data; name=\"status\"\r\n\r\nactive\r\n" +
      "------WebKitFormBoundaryOrtvYGBXE2gxan8t\r\nContent-Disposition: form-data; name=\"description\"\r\n\r\n\r\n" +
      "------WebKitFormBoundaryOrtvYGBXE2gxan8t\r\nContent-Disposition: form-data; name=\"tpe\"\r\n\r\n\r\n" +
      "------WebKitFormBoundaryOrtvYGBXE2gxan8t\r\nContent-Disposition: form-data; name=\"spaceId\"\r\n\r\n\r\n" +
      "------WebKitFormBoundaryOrtvYGBXE2gxan8t\r\nContent-Disposition: form-data; name=\"roleFilter\"\r\n\r\non\r\n" +
      "------WebKitFormBoundaryOrtvYGBXE2gxan8t\r\nContent-Disposition: form-data; name=\"dtStart\"\r\n\r\n\r\n" +
      "------WebKitFormBoundaryOrtvYGBXE2gxan8t\r\nContent-Disposition: form-data; name=\"dtEnd\"\r\n\r\n\r\n" +
      "------WebKitFormBoundaryOrtvYGBXE2gxan8t--\r\n'"
    val result = command.!!.trim
    assertEquals(result, "Received multipart request with files: 0")
  }

  test("POST with bad request") {
    val mpReq =
      val bodyPart = BodyPart.of(ContentDisposition.of("form-data", "name"), "homer")
      Multipart.of(bodyPart)
        .toHttpRequest(s"http://localhost:${server.port}/multipart-bad-request")
    val resp = WebClient.of().execute(mpReq).aggregate().join
    assertEquals(resp.status(), HttpStatus.BAD_REQUEST)
    assertNoDiff(resp.contentUtf8(), "Bad Request")
  }

  test("POST with bad request 2") {
    val mpReq =
      val bodyPart = BodyPart.of(ContentDisposition.of("form-data", "name"), "homer")
      Multipart.of(bodyPart)
        .toHttpRequest(s"http://localhost:${server.port}/multipart-bad-request-2")
    val resp = WebClient.of().execute(mpReq).aggregate().join
    assertEquals(resp.status(), HttpStatus.BAD_REQUEST)
    assertNoDiff(resp.contentUtf8(), "Bad Request 2")
  }

  test("POST with internal server error") {
    val mpReq =
      val bodyPart = BodyPart.of(ContentDisposition.of("form-data", "name"), "homer")
      Multipart.of(bodyPart)
        .toHttpRequest(s"http://localhost:${server.port}/multipart-internal-server-error")
    val resp = WebClient.of().execute(mpReq).aggregate().join
    assertEquals(resp.status(), HttpStatus.INTERNAL_SERVER_ERROR)
    assertNoDiff(resp.contentUtf8(), "Internal Server Error")
  }

}
