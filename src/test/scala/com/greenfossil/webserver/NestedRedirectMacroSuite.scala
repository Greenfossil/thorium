package com.greenfossil.webserver

import com.linecorp.armeria.server.annotation.Get

object NestedRedirectServices extends Controller {

  @Get("/nestedRedirect")
  def nestedRedirect = Action { implicit request2 =>
    println("This is a println")
    //Reference class' action
    nestedRedirectFn(actionEnd)
  }

  inline def nestedRedirectFn(inline a: Action): Result =
    Redirect(a)


  @Get("/actionEnd")
  def actionEnd = Action {implicit request =>
    Ok("End")
  }

}

class NestedRedirectMacroSuite extends munit.FunSuite {


}
