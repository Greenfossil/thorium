package com.greenfossil.thorium

class RegexEndpointSuite extends munit.FunSuite{
  import examples.ParameterizedServices
  //This test suite references com.greenfossil.webserver.examples.ParameterizedServices
  test("caseInsensitivity"){
    val ep = EndpointMcr(ParameterizedServices.caseInsensitivity("homer"))
    assertNoDiff(ep.path, "/ci/homer")
  }

  test("caseInsensitivity2") {
    val ep = EndpointMcr(ParameterizedServices.caseInsensitivity2("homer"))
    assertNoDiff(ep.path, "/ci2/homer")
  }

  test("caseInsensitivity3") {
    val ep = EndpointMcr(ParameterizedServices.caseInsensitivity3("homer"))
    assertNoDiff(ep.path, "/ci3/homer")
  }

  test("caseInsensitivity4") {
    val ep = EndpointMcr(ParameterizedServices.caseInsensitivity4("homer"))
    assertNoDiff(ep.path, "/ci4/homer")
  }

}
