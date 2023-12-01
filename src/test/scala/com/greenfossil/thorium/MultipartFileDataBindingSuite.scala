package com.greenfossil.thorium

import com.greenfossil.data.mapping.Mapping.{*, given}
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.common.ContentDisposition
import com.linecorp.armeria.common.multipart.{BodyPart, Multipart, MultipartFile}
import com.linecorp.armeria.common.stream.StreamMessage
import com.linecorp.armeria.server.annotation.Post

import java.nio.file.Paths

class MultipartFileDataBindingSuite extends munit.FunSuite:


  object FormServices:
    @Post("/multipart")
    def multipartForm: Action = Action.multipart { implicit request =>
      //Test the Multipart File data-binding using NotEmptyText then followed by Transformation[String, MultipartFile]
      val nameField = nonEmptyText.name("name")
      val fileField = nonEmptyText.name("file")
        .verifying("File name must be 'logback-test.xml'", _ == "logback-test.xml2")
        .transform[MultipartFile](name => request.findFileOfFileName(name).orNull, file => file.filename())
        .verifying("File size cannot be more than 10 bytes", f => f.sizeInBytes < 10)

      val nameValueOpt = nameField.bindFromRequest().typedValueOpt
      assertNoDiff(nameValueOpt.orNull, "homer")
      println(s" = ${nameValueOpt}")

      //file assertions
      val fileBindForm = fileField.bindFromRequest()
      assertEquals(fileBindForm.errors.size, 2)
      println(s"Errors ${fileBindForm.errors.size}")
      fileBindForm.errors foreach println

      assert(fileBindForm.typedValueOpt.nonEmpty)
      println(s"fileBindForm.typedValueOpt = ${fileBindForm.typedValueOpt}")

      "Received"
  }

  test("POST with file content") {
    //Start
    val server = Server()
      .addServices(FormServices)
      .start()

    val namePart = BodyPart.of(ContentDisposition.of("form-data", "name"), "homer")
    val path = Paths.get("src/test/resources/logback-test.xml").toAbsolutePath
    val filePart = BodyPart.of(ContentDisposition.of("form-data", "file", "logback-test.xml"),
      StreamMessage.of(path))
    val multipart = Multipart.of(namePart, filePart)
    val resp = WebClient.of(s"http://localhost:${server.port}")
      .execute(multipart.toHttpRequest("multipart"))
      .aggregate()
      .get()
    assertNoDiff(resp.contentUtf8(), "Received")

    //Stop server
    val status = server.stop().value
  }

