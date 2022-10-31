package com.greenfossil.thorium

import com.linecorp.armeria.server.annotation.{Get, Param}

class EndpointMacro2Suite extends munit.FunSuite {

  @Get("/endpoint-params/:int/:str")
  def endpointParams(@Param int: Int)(@Param str: String) = "endpoint1"

  @Get("/endpoint-using")
  def endpointUsing(using request: Request) =
    s"endpoint Int:${request.path}"

  test("multi-param-list") {
    val epParams = endpointParams(1)("string").url
    assertNoDiff(epParams, "/endpoint-params/1/string")
  }

  test("using"){
    given Request = null
    val epUsing = endpointUsing.url
    assertNoDiff(epUsing, "/endpoint-using")
  }

}
