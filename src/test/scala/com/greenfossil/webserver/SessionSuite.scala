package com.greenfossil.webserver

import com.linecorp.armeria.common.{HttpMethod, HttpRequest, HttpStatus}
import com.linecorp.armeria.server.annotation.Get

object SessionServices extends Controller {
  @Get("/s0")
  def s0 = Action { request =>
    println(s"S0 request.session = ${request.session}")
    Redirect("/s1", HttpStatus.SEE_OTHER).withNewSession.flashing("News Flash" ->"Flash Value")
  }

  @Get("/s1")
  def s1 = Action { request =>
    println(s"S1 request.session = ${request.session}, flash ${request.flash}")
    Redirect("/s2").withSession(request.session + ("foo" -> "bar"))
  }

  @Get("/s2")
  def s2 = Action {request =>
    println(s"S2 request.session = ${request.session}, flash ${request.flash}") //foo->bar
    Redirect("/s3").withSession(request.session + ("baz" -> "foobaz"))
  }

  @Get("/s3")
  def s3 = Action {request =>
    println(s"S3 request.session = ${request.session}, flash ${request.flash}") //foo->bar, "baz" -> foobaz
    Ok(s"stop ${request.session}").withSession(request.session + ("baz" -> "foobaz"))
  }
}

@main def main2 =
    val server = WebServer(8080)
      .addAnnotatedService(SessionServices)
      .start()

//class SessionSuite extends munit.FunSuite {
//
//  test("Session propagation") {
//
//    val server = WebServer()
//      .addAnnotatedService(SessionServices)
//      .start()
//
//    import com.linecorp.armeria.client.WebClient
//    val client = WebClient.of(s"http://localhost:${server.port}")
//    val resp = client.get("/s0")
////    val resp = HttpRequest.of(HttpMethod.GET, "/s0")
//    resp.aggregate().thenApply{ aggResp =>
//      println("Response...")
//      println(s"aggResp.status() = ${aggResp.status()}")
//      println(s"aggResp.contentUtf8() = ${aggResp.contentUtf8()}")
//      Thread.sleep(1000)
//      server.stop()
//    }
//  }
//
//}
