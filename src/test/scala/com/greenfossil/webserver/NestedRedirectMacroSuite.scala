package com.greenfossil.webserver

import com.linecorp.armeria.server.annotation.Get

object NestedRedirectServices extends Controller {

  @Get("/nestedRedirect")
  def nestedRedirect = Action { implicit request2 =>
    println("This is a println")
    //Reference class' action
    nestedRedirectFn(Endpoint(actionEnd))
  }

  def nestedRedirectFn(ep: Endpoint): Result =
    Redirect(ep)


  @Get("/actionEnd")
  def actionEnd = Action{ implicit request =>
    Ok("End")
  }

}

class NestedRedirectMacroSuite extends munit.FunSuite {

  test("Endpoint.apply()") {
    val ep = Endpoint(NestedRedirectServices.actionEnd)
    assertNoDiff(ep.url, "/actionEnd")
  }


}
