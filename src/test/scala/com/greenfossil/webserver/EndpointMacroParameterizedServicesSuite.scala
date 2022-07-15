package com.greenfossil.webserver

import com.greenfossil.webserver.examples.ParameterizedServices
import com.linecorp.armeria.server.annotation.{Get, Param, Path, Post}

import java.time.LocalDateTime

/*
 *Test path with prefix, regex, glob
 */
class EndpointMacroParameterizedServicesSuite extends munit.FunSuite {

  test("prefix endpoint") {
    val prefixEp = EndpointFnMcr(ParameterizedServices.prefixEndpoint)
    assertNoDiff(prefixEp.url, "/howdy")
    assertNoDiff(prefixEp.method, "Get")
  }

  test("regex string endpoint") {
    val regexStringEp = EndpointFnMcr(ParameterizedServices.regexStringEndpoint("homer"))
    assertNoDiff(regexStringEp.url, "/string/homer")
    assertNoDiff(regexStringEp.method, "Get")
  }

  test("regex number endpoint") {
    val regexNumEp = EndpointFnMcr(ParameterizedServices.regexNumberEndpoint(5))
    assertNoDiff(regexNumEp.url, "/number/5")
    assertNoDiff(regexNumEp.method, "Get")
  }

  test("regex string2 endpoint") {
    val regexString2Ep = EndpointFnMcr(ParameterizedServices.regexString2Endpoint("min", "max"))
    assertNoDiff(regexString2Ep.url, "/string2/min/max")
    assertNoDiff(regexString2Ep.method, "Get")
  }

  test("regex number2 endpoint") {
    val regexNumber2Ep = EndpointFnMcr(ParameterizedServices.regexNumber2Endpoint(10, 20))
    assertNoDiff(regexNumber2Ep.url, "/number2/10/20")
    assertNoDiff(regexNumber2Ep.method, "Get")
  }

  test("regex mix endpoint") {
    val regexMix1Ep = EndpointFnMcr(ParameterizedServices.regexMix1Endpoint(10, "last"))
    assertNoDiff(regexMix1Ep.url, "/mix/10/last")
    assertNoDiff(regexMix1Ep.method, "Get")
  }

  test("regex mix2 endpoint") {
    val regexMix2Ep = EndpointFnMcr(ParameterizedServices.regexMix2Endpoint("first", 20))
    assertNoDiff(regexMix2Ep.url, "/mix/first/20")
    assertNoDiff(regexMix2Ep.method, "Get")
  }

//  test("glob endpoint") {
//    val globEp = EndpointFnMcr(ParameterizedServices.globEndpoint("before", "after"))
//    assertNoDiff(globEp.url, "/glob/before/hello/after")
//    assertNoDiff(globEp.method, "Get")
//    assertEquals(globEp.queryParams.size, 1)
//  }
}
