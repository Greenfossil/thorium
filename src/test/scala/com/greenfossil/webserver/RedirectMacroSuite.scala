package com.greenfossil.webserver

import com.linecorp.armeria.server.annotation.{Get, Param}

object Redirect1Services extends Controller {

  @Get("/action1")
  def action1 = Action { implicit request =>
    Redirect(action2("World!"))
  }

  @Get("/action2/:name")
  def action2(@Param name: String) = Action { implicit request =>
    s"Hello $name"
  }
}

object Redirect2Services extends Controller {
  @Get("/action3")
  def action3 = Action { implicit request =>
    Redirect(action4)
  }

  @Get("/action4")
  def action4 = Action { implicit request =>
    Redirect(Redirect1Services.action2("Space!"))
  }

}

@main def redirectMain =
    val server = WebServer(8080)
      .addAnnotatedService(Redirect1Services)
      .addAnnotatedService(Redirect2Services)
      .start()