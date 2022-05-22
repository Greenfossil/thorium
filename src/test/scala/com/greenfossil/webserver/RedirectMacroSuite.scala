package com.greenfossil.webserver

import com.linecorp.armeria.server.annotation.{Get, Param}

object Redirect1Services extends Controller {

  @Get("/action1")
  def action1 = Action { request =>
    //Redirect with Ref and Constant
    val time = java.time.LocalDate.now.toString
    Redirect(action2(time, "World!"))
  }

  @Get("/action2/:name/:msg")
  def action2(@Param name: String, @Param msg: String) = Action { request =>
    s"Hello name:$name, msg:$msg"
  }
}

object Redirect2Services extends Controller {
  @Get("/action3")
  def action3 = Action { request =>
    Redirect(action4)
  }

  @Get("/action4")
  def action4 = Action { request =>
    //Redirect with Ref - name, and inline
    val name = "Space!"
    Redirect(Redirect1Services.action2(name, java.time.LocalDateTime.now.toString))
  }

}

@main def redirectMain =
    val server = WebServer(8080)
      .addServices(Redirect1Services)
      .addServices(Redirect2Services)
      .start()