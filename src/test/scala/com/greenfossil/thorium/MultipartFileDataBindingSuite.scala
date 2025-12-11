package com.greenfossil.thorium

import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.common.multipart.MultipartFile
import com.linecorp.armeria.server.annotation.Post
import io.github.yskszk63.jnhttpmultipartformdatabodypublisher.MultipartFormDataBodyPublisher

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.file.Paths
import scala.concurrent.duration.Duration

class MultipartFileDataBindingSuite extends munit.FunSuite:

  override def munitTimeout: Duration = Duration("1h")

  object FormServices:
    @Post("/multipart")
    def multipartForm: Action = Action.multipart { implicit request =>
      //Test the Multipart File data-binding using NotEmptyText then followed by Transformation[String, MultipartFile]
      val nameField = nonEmptyText.name("name")
      val fileField = nonEmptyText.name("file")
        .verifying("File name must be 'logback-test.xml'", _ == "logback-test.xml2")
        .transform[MultipartFile](name => request.findFile((fieldName, fileName, _, _) => fileName == name).getOrElse(null), file => file.filename())
        .verifying("File size cannot be more than 10 bytes", f => f.sizeInBytes < 10)

      val nameValueOpt = nameField.bindFromRequest().typedValueOpt
      assertNoDiff(nameValueOpt.orNull, "homer")

      //file assertions
      val fileBindForm = fileField.bindFromRequest()
      assertEquals(fileBindForm.errors.size, 2)

      assert(fileBindForm.typedValueOpt.nonEmpty)

      "Received"
  }

  test("POST with file content") {
    //Start
    val server = Server(0)
      .addServices(FormServices)
      .start()

    val path = Paths.get("src/test/resources/logback-test.xml").toAbsolutePath
    val mpPub = MultipartFormDataBodyPublisher()
      .add("name", "homer")
      .addFile("file", path, "application/xml")
    val resp = HttpClient.newHttpClient()
      .send(
        HttpRequest.newBuilder(URI.create(s"http://localhost:${server.port}/multipart"))
          .POST(mpPub)
          .header("Content-Type", mpPub.contentType())
          .build(),
        HttpResponse.BodyHandlers.ofString()
      )
    assertNoDiff(resp.body(), "Received")
    //Stop server
    server.stop()
  }

