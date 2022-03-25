package com.greenfossil.webserver

import com.linecorp.armeria.common.{Cookie, HttpStatus}
import com.linecorp.armeria.server.annotation.Get

object HeadersServices extends Controller {
  @Get("/headers")
  def headers = Action { request =>
    println(s"S0 request.session = ${request.session}")
    Ok("Headers sent")
      .withHeaders(
        "Access-Control-Allow-Origin" -> "*",
        "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept")
      .withSession("session" -> "sessionValue")
      .flashing("flash" -> "flashValue")
      .withCookies(Cookie.of("Cookie1", "Cookie1Value"), Cookie.of("Cookie2", "CookieValue2"))
  }

}

//@main def headersMain =
//  val server = WebServer(8080)
//    .addAnnotatedService(HeadersServices)
//    .start()

class HeadersSuite extends munit.FunSuite {
  test("header, session, flash"){
    val server = WebServer()
      .addAnnotatedService(HeadersServices)
      .start()

    import com.linecorp.armeria.client.WebClient
    val client = WebClient.of(s"http://localhost:${server.port}")
    val resp = client.get("/headers")
    resp.aggregate().thenApply{ aggResp =>
      println(s"aggResp.headers() = ${aggResp.headers()}")
      assertEquals(aggResp.status(), HttpStatus.OK)
      assertNoDiff(aggResp.contentUtf8(), "Headers sent")
    }.join()
    server.server.stop()
  }

}