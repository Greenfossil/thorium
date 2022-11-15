package com.greenfossil.thorium

import com.linecorp.armeria.server.annotation.{Get, Param}

class EndpointMacro2Suite extends munit.FunSuite {

  @Get("/endpoint-params/:int/:str")
  def endpointParams(@Param int: Int)(@Param str: String) = "endpoint1"

  @Get("/endpoint-using")
  def endpointUsing(using request: Request) =
    s"endpoint Int:${request.path}"

  @Get("/endpoint-redirect1")
  def endpointRedirect1(using request: Request) =
    Redirect(endpointRedirect2)

  @Get("/endpoint-redirect2")
  def endpointRedirect2(using request: Request) =
    "Howdy"

  @Get("/endpoint-redirect3")
  def endpointRedirect3(using request: Request) =
    Redirect(endpointRedirect4(1))

  @Get("/endpoint-redirect4")
  def endpointRedirect4(int: Int)(using request: Request) =
    "Howdy2"

  test("multi-param-list") {
    val epParams = endpointParams(1)("string").endpoint.url
    assertNoDiff(epParams, "/endpoint-params/1/string")
  }

  test("using"){
    given Request = null
    val epUsing = endpointUsing.endpoint.url
    assertNoDiff(epUsing, "/endpoint-using")
  }

}
