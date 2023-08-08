package com.greenfossil.thorium

import com.greenfossil.thorium.decorators.CSRFProtectionDecoratingFunction
import com.linecorp.armeria.client.*
import com.linecorp.armeria.common.*

import java.net.URLEncoder
import java.time.Duration

class CSRFProtectionDecoratingFunction2Suite extends munit.FunSuite:

  test("SameOrigin POST /csrf/email/change"):
    doPost(target => target)


  test("Null Origin POST /csrf/email/change"):
    doPost(target => null)


  test("CrossOrigin POST /csrf/email/change"):
    doPost(target => "http://another-site")

  private def doPost(originFn: String => String)(using loc:munit.Location) =
    val server = Server(0)
      .addServices(CSRFServices)
      .serverBuilderSetup(sb => {
        sb.routeDecorator()
          .pathPrefix("/")
          .build(CSRFProtectionDecoratingFunction())
      })
      .start()
    
    val postEpPath = "/csrf/email/change"
    val csrfCookieTokenName = Configuration().httpConfiguration.csrfConfig.cookieName
    val target = s"http://localhost:${server.port}"
    val client = WebClient.of(target)
    val csrfCookie = CSRFProtectionDecoratingFunction.generateCSRFTokenCookie(Configuration(), Some("ABC"))
    val content = s"email=password&${csrfCookieTokenName}=${URLEncoder.encode(csrfCookie.value(), "UTF-8")}"
    
    //Set up headers
    val headersBuilder = RequestHeaders.builder(HttpMethod.POST, postEpPath)
      .contentType(MediaType.FORM_DATA)
      .contentLength(content.length)
      .cookies(csrfCookie)
    
    //Set origin
    Option(originFn(target)).map(target => headersBuilder.set("Origin", target))
    
    //Build headers
    val headers = headersBuilder.build()
    
    //Build request
    val request = HttpRequest.of(headers, HttpData.ofUtf8(content))
    
    //Set response timeout
    val reqOpts = RequestOptions.builder().responseTimeout(Duration.ofHours(1)).build()
    
    val postResp = client.execute(request, reqOpts).aggregate().join()
    assertNoDiff(postResp.status().codeAsText(), "200")
    assertNoDiff(postResp.contentUtf8(), "Password Changed")
    
    server.stop()