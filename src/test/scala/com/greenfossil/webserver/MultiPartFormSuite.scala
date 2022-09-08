package com.greenfossil.webserver

import com.greenfossil.webserver.examples.FormServices
import munit.FunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.sys.process.*

class MultiPartFormSuite extends FunSuite{

  var port: Int = 8080

  override def beforeAll(): Unit = {
    WebServer(port).addServices(FormServices).start()
    println(s"Server started... ${Thread.currentThread()}")
  }

  test("POST with file content") {
    Files.write(Paths.get("/tmp/file.txt"), "Hello world".getBytes(StandardCharsets.UTF_8))

    val result = "curl http://localhost:8080/multipart3 -F resourceFile=@/tmp/file.txt ".!!.trim
    println(s"Result: [$result]")
    assertEquals(result, "Received multipart request with files: 1")
  }

  test("POST without file content but with form param") {
    val result = "curl http://localhost:8080/multipart3 -F name=homer ".!!.trim
    println(s"Result: [${result}]")
    assertEquals(result, "Received multipart request with files: 0")
  }

  test("POST without file content using web browser's raw data") {
    val result =
      "curl http://localhost:8080/multipart3 -H 'Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryaDaB4MtEkj4a1pYx' --data-raw $'------WebKitFormBoundaryaDaB4MtEkj4a1pYx--\r\n'".!!.trim
    println(s"Result: [${result}]")
    assertEquals(result, "Received multipart request with files: 0")
  }

  test("POST without file content but with form param, using web browser's raw data") {
    val command = "curl http://localhost:8080/multipart3 -H 'Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryOrtvYGBXE2gxan8t' " +
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
    println(s"Result: [${result}]")
    assertEquals(result, "Received multipart request with files: 0")
  }

}
