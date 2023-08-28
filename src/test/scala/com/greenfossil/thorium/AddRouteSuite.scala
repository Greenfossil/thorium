package com.greenfossil.thorium

import com.linecorp.armeria.common.MediaType

//class AddRouteSuite extends munit.FunSuite {
object AddRouteSuite:

  var server: Server = null

//  override def beforeAll(): Unit =
  @main
  def test =
    server = Server(8080)
      .addRoute: route =>
        route.get("/route/greetings/:name").build:
          Action { req =>
            val name = req.pathParam("name")
            s"<h1>Greetings ${name}!</h1>".as(MediaType.HTML_UTF_8)
          }
      .addRoute: route =>
        route.post("/route/howdy").consumes(MediaType.FORM_DATA).build:
          Action { implicit req =>
            val form: FormUrlEndcoded = req.asFormUrlEncoded
            s"Howdy ${form.getFirst("name")}"
          }
      .addRoute: route =>
        route.get("/route/welcome/:name").build:
          Action { implicit req =>
            import com.greenfossil.htmltags.*
            Ok(h2(s"Welcome! ${req.pathParam("name")}"))
          }
      .start()


