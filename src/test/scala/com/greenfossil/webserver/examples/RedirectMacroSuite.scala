package com.greenfossil.webserver.examples

import com.greenfossil.webserver.*
import com.linecorp.armeria.server.annotation.{Get, Param}

@main def redirectMain =
  val server = WebServer(8080)
    .addServices(Redirect1Services)
    .addServices(Redirect2Services)
    .start()

object Redirect1Services extends Controller {

  //curl http://localhost:8080/action1 -L
  @Get("/action1")
  def action1 = Action { request =>
    //Redirect with Ref and Constant
    def time = java.time.LocalDate.now.toString
    Redirect(action2(time, "World!"))
  }

  @Get("/action2/:value/:msg")
  def action2(@Param value: String, @Param msg: String) = Action { request =>
    s"Hello value:$value, msg:$msg"
  }
}

object Redirect2Services extends Controller {

  /*
   * curl http://localhost:8080/action3 -L
   */
  @Get("/action3")
  def action3 = Action { request =>
    Redirect(action4)
  }

  @Get("/action4")
  def action4 = Action { request =>
    //Redirect with Ref - name, and inline
    def name = "Space!"
    Redirect(Redirect1Services.action2(name, java.time.LocalDateTime.now.toString))
  }

}
