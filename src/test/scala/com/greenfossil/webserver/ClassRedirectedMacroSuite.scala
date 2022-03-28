package com.greenfossil.webserver

import com.linecorp.armeria.server.annotation.*

object SingletonService extends Controller {

    @Get("/classRefAction")
    def classRefAction = Action { implicit request =>
      //Reference class' action
      Redirect[ClassServices](_.action1("Howdy!"))
    }

}

class ClassServices extends Controller {
  @Get("/action1/:name")
  def action1(@Param name: String) = Action { implicit request =>
    Redirect(action2("Class Service"))
  }

  @Get("/action2/:msg")
  def action2(@Param msg: String) = Action { implicit request =>
    Ok(s"Class $msg")
  }

}

class ClassRedirectedMacroSuite extends munit.FunSuite{

}
