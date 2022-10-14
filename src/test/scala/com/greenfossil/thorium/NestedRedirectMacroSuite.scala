package com.greenfossil.thorium

import com.greenfossil.thorium.{*, given}
import com.linecorp.armeria.server.annotation.Get

object NestedRedirectServices extends Controller {

  @Get("/nestedRedirect")
  def nestedRedirect = Action { request2 =>
    //Reference class' action
    nestedRedirectFn(actionEnd.endpoint)
  }

  def nestedRedirectFn(ep: Endpoint): Result =
    Redirect(ep)

  @Get("/actionEnd")
  def actionEnd = Action{ request =>
    Ok("End")
  }

}

class NestedRedirectMacroSuite extends munit.FunSuite {

  test("Endpoint.apply()") {
    val ep = NestedRedirectServices.actionEnd.endpoint
    assertNoDiff(ep.url, "/actionEnd")
  }

}
