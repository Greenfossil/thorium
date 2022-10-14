package com.greenfossil.thorium

import com.greenfossil.thorium.examples.ParameterizedServices
import com.linecorp.armeria.server.annotation.{Get, Param, Path, Post}

import java.time.LocalDateTime

/*
 *Test path with prefix, regex, glob
 */
class  EndpointMacroParameterizedServicesSuite extends munit.FunSuite {

  test("prefix endpoint") {
    val prefixEp = EndpointMcr(ParameterizedServices.prefixEndpoint)
    assertNoDiff(prefixEp.url, "/howdy")
    assertNoDiff(prefixEp.method, "Get")
  }

  test("braced param"){
    val bracedEndpoint = EndpointMcr(ParameterizedServices.bracedParams("homer simpson",42, "spring/field"))
    assertNoDiff(bracedEndpoint.url, "/braced-params/homer%20simpson/42/spring%2Ffield")
  }

  test("regex string endpoint") {
    val regexStringEp = EndpointMcr(ParameterizedServices.regexStringEndpoint("homer"))
    assertNoDiff(regexStringEp.url, "/string/homer")
    assertNoDiff(regexStringEp.method, "Get")
  }

  test("regex number endpoint") {
    def num = 5
    val regexNumEp = EndpointMcr(ParameterizedServices.regexNumberEndpoint(num))
    assertNoDiff(regexNumEp.url, "/number/5")
    assertNoDiff(regexNumEp.method, "Get")
  }

  test("regex string2 endpoint") {
    def minValue = "min"
    def maxValue = "max"
    val regexString2Ep = EndpointMcr(ParameterizedServices.regexString2Endpoint(minValue, maxValue))
    assertNoDiff(regexString2Ep.url, "/string2/min/max")
    assertNoDiff(regexString2Ep.method, "Get")
  }

  test("regex number2 endpoint") {
    def arg1 = 10
    def arg2 = 20
    val regexNumber2Ep = EndpointMcr(ParameterizedServices.regexNumber2Endpoint(arg1, arg2))
    assertNoDiff(regexNumber2Ep.url, "/number2/10/20")
    assertNoDiff(regexNumber2Ep.method, "Get")
  }

  test("regex mix endpoint") {
    def arg1 = 10
    def arg2 = "last"
    val regexMix1Ep = EndpointMcr(ParameterizedServices.regexMix1Endpoint(arg1, arg2))
    assertNoDiff(regexMix1Ep.url, "/mix/10/last")
    assertNoDiff(regexMix1Ep.method, "Get")
  }

  test("regex mix2 endpoint") {
    def arg1 = "first"
    def arg2 = 20
    val regexMix2Ep = EndpointMcr(ParameterizedServices.regexMix2Endpoint(arg1, arg2))
    assertNoDiff(regexMix2Ep.url, "/mix/first/20")
    assertNoDiff(regexMix2Ep.method, "Get")
  }

//  test("glob endpoint") {
//    val globEp = EndpointMcr(ParameterizedServices.globEndpoint("before", "after"))
//    assertNoDiff(globEp.url, "/glob/before/hello/after")
//    assertNoDiff(globEp.method, "Get")
//    assertEquals(globEp.queryParams.size, 1)
//  }
}
