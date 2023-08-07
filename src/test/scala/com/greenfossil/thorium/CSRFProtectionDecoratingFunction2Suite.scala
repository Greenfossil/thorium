package com.greenfossil.thorium

import com.greenfossil.thorium.decorators.CSRFProtectionDecoratingFunction
import com.linecorp.armeria.client.*
import com.linecorp.armeria.common.*

import java.net.URLEncoder
import java.time.Duration

class CSRFProtectionDecoratingFunction2Suite extends munit.FunSuite:

  private val postEpPath = "/csrf/email/change"
  private val csrfCookieTokenName = Configuration().httpConfiguration.csrfConfig.cookieName

  test("SameOrigin POST /csrf/email/change") {
    val server = Server(0)
      .addServices(CSRFServices)
      .addCSRFProtection()
      .start()
    val target = s"http://localhost:${server.port}"
    val client = WebClient.of(target)
    val csrfCookie = CSRFProtectionDecoratingFunction.generateCSRFTokenCookie(Configuration(), Some("ABC"))
    val content = s"email=password&${csrfCookieTokenName}=${URLEncoder.encode(csrfCookie.value(), "UTF-8")}"
    println(s"content = ${content}")
    val headers = RequestHeaders.builder(HttpMethod.POST, postEpPath)
      .contentType(MediaType.FORM_DATA)
      .contentLength(content.length)
      .cookies(csrfCookie)
      .set("Origin", target)
      .build()
    val request = HttpRequest.of(headers, HttpData.ofUtf8(content))
    val reqOpts = RequestOptions.builder()
      .responseTimeout(Duration.ofHours(1))
      .build()
    val postResp = client.execute(request, reqOpts).aggregate().join()
    assertNoDiff(postResp.status().codeAsText(), "200")
    assertNoDiff(postResp.contentUtf8(), "Password Changed")
    server.stop()
  }

  test("Null Origin POST /csrf/email/change") {
    val server = Server(0)
      .addServices(CSRFServices)
      .serverBuilderSetup(sb => {
        sb.routeDecorator()
          .pathPrefix("/")
          .build(CSRFProtectionDecoratingFunction())
      })
      .start()

    val client = WebClient.of(s"http://localhost:${server.port}")
    val csrfCookie = CSRFProtectionDecoratingFunction.generateCSRFTokenCookie(Configuration(), Some("ABC"))
    val content = s"email=password&${csrfCookieTokenName}=${URLEncoder.encode(csrfCookie.value(), "UTF-8")}"
    val headers = RequestHeaders.builder(HttpMethod.POST, postEpPath)
      .contentType(MediaType.FORM_DATA)
      .contentLength(content.length)
      .cookies(csrfCookie)
      .build()
    val request = HttpRequest.of(headers, HttpData.ofUtf8(content))
    val reqOpts = RequestOptions.builder()
      .responseTimeout(Duration.ofHours(1))
      .build()
    val postResp = client.execute(request, reqOpts).aggregate().join()
    assertNoDiff(postResp.status().codeAsText(), "200")
    assertNoDiff(postResp.contentUtf8(), "Password Changed")
    server.stop()
  }

  test("CrossOrigin POST /csrf/email/change") {
    val server = Server(0)
      .addServices(CSRFServices)
      .serverBuilderSetup(sb => {
        sb.routeDecorator()
          .pathPrefix("/")
          .build(CSRFProtectionDecoratingFunction())
      })
      .start()
    val client = WebClient.of(s"http://localhost:${server.port}")
    val csrfCookie = CSRFProtectionDecoratingFunction.generateCSRFTokenCookie(Configuration(), Some("ABC"))
    val content = s"email=password&${csrfCookieTokenName}=${URLEncoder.encode(csrfCookie.value(), "UTF-8")}"
    println(s"content = ${content}")
    val headers = RequestHeaders.builder(HttpMethod.POST, postEpPath)
      .contentType(MediaType.FORM_DATA)
      .contentLength(content.length)
      .cookies(csrfCookie)
      .set("Origin", "http://another-site")
      .build()
    val request = HttpRequest.of(headers, HttpData.ofUtf8(content))
    val reqOpts = RequestOptions.builder()
      .responseTimeout(Duration.ofHours(1))
      .build()
    val postResp = client.execute(request, reqOpts).aggregate().join()
    assertNoDiff(postResp.status().codeAsText(), "200")
    assertNoDiff(postResp.contentUtf8(), "Password Changed")
    server.stop()
  }