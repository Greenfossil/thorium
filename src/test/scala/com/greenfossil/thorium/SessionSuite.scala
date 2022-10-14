package com.greenfossil.thorium

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.client.RequestOptions
import com.linecorp.armeria.client.cookie.CookieClient
import com.linecorp.armeria.client.logging.LoggingClient
import com.linecorp.armeria.common.{HttpMethod, HttpRequest, HttpStatus}
import com.linecorp.armeria.server.annotation.Get

object SessionServices extends Controller {
  @Get("/s0")
  def s0 = Action { request =>
    Redirect("/s1", HttpStatus.SEE_OTHER).withNewSession.flashing("News Flash" ->"Flash Value")
  }

  @Get("/s1")
  def s1 = Action { request =>
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
    Ok(s"S3 reached ${request.session}").withSession(request.session + ("baz" -> "foobaz"))
  }
}

class SessionSuite extends munit.FunSuite {

  test("Session, Flash propagation") {
    val server = WebServer()
      .addServices(SessionServices)
      .start()

    import com.linecorp.armeria.client.WebClient
    import com.linecorp.armeria.client.cookie.*
    val client = WebClient
      .builder(s"http://localhost:${server.port}")
      .followRedirects()
      .decorator(CookieClient.newDecorator(CookiePolicy.acceptAll()))
      .decorator(LoggingClient.newDecorator())
      .build()
    val resp = client.get("/s0")
    resp.aggregate().thenAccept{ aggResp =>
      assert(aggResp.contentUtf8().startsWith("S3 reached"))
    }
    server.server.stop()
  }

}
