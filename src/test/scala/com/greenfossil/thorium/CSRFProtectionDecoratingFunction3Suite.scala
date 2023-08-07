package com.greenfossil.thorium

import com.greenfossil.thorium.decorators.CSRFProtectionDecoratingFunction
import com.linecorp.armeria.client.*
import com.linecorp.armeria.common.*
import com.linecorp.armeria.common.multipart.{BodyPart, Multipart}
import com.linecorp.armeria.common.stream.StreamMessage

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Duration

class CSRFProtectionDecoratingFunction3Suite extends munit.FunSuite:

  private val postEpPath = "/csrf/multipart-file"
  private val csrfCookieTokenName = Configuration().httpConfiguration.csrfConfig.cookieName

  test("SameOrigin POST with file content") {
    val server = Server(0)
      .addServices(CSRFServices)
      .addCSRFProtection()
      .serverBuilderSetup(_.requestTimeout(Duration.ofHours(1)))
      .start()
    val port = server.port
    val target = s"http://localhost:${port}"
    val client = WebClient.of(target)
    //Get the CSRFToken cookie
    val csrfCookie = CSRFProtectionDecoratingFunction.generateCSRFTokenCookie(Configuration(), Some("ABC"))
    val formPart = BodyPart.of(ContentDisposition.of("form-data", "name"), "Homer")
    val csrfPart = BodyPart.of(ContentDisposition.of("form-data", csrfCookieTokenName), csrfCookie.value())
    val filePath = Files.write(Paths.get("/tmp/file.txt"), "Hello world".getBytes(StandardCharsets.UTF_8))
    val filePart = BodyPart.of(ContentDisposition.of("form-data", "file", "file.txt"), StreamMessage.of(filePath))
    val multipart = Multipart.of(csrfPart, formPart, filePart)
    val multipartRequest = multipart.toHttpRequest(postEpPath)

    val csrfMultipartRequest = multipartRequest.mapHeaders( headers => headers.toBuilder.cookies(csrfCookie).set("Origin", target).build())
    val reqOpts = RequestOptions.builder()
      .responseTimeout(Duration.ofHours(1))
      .build()
    val postResp = client.execute(csrfMultipartRequest, reqOpts).aggregate().join()
    println(s"postResp.status() = ${postResp.status()}")
    println(s"postResp.contentUtf8() = ${postResp.contentUtf8()}")
    assertNoDiff(postResp.status().codeAsText(), "200")
    assertNoDiff(postResp.contentUtf8(), s"Received multipart request with files: 1, form:FormUrlEndcoded(Map(name -> List(Homer), APP_CSRF_TOKEN -> List(${csrfCookie.value()})))")
    server.stop()
  }

