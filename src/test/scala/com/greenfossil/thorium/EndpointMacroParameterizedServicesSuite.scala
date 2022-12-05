/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greenfossil.thorium

import com.linecorp.armeria.server.annotation.{Get, Param, Path, Post}

import java.time.LocalDateTime

/*
 *Test path with prefix, regex, glob
 */
class  EndpointMacroParameterizedServicesSuite extends munit.FunSuite {
  object ParameterizedServices {
    @Get("prefix:/howdy")
    def prefixEndpoint = "prefix - howdy"
    
    @Get("/braced-params/{name}/{age}/:contact")
    def bracedParams(@Param name: String, @Param age: Int, @Param contact: String) =
      s"braced-param name:${name} age:${age} contact:${contact}"
    
    @Get("regex:^/string/(?<name>[^0-9]+)$") //This path is different without ^$ "regex:/string/(?<name>[^0-9]+)"
    def regexStringEndpoint(@Param name: String) = s"regexString - name:$name"
    
    @Get("regex:^/number/(?<n1>[0-9]+)$") 
    def regexNumberEndpoint(@Param n1: Int) = s"regexNumber - $n1"
    
    @Get("regex:/string2/(?<min>\\w+)/(?<max>\\w+)")
    def regexString2Endpoint(@Param min: String, @Param max: String) = s"regexString2 - min:$min - max:$max"

    @Get("regex:/number2/(?<min>\\d+)/(?<max>\\d+)")
    def regexNumber2Endpoint(@Param min: Int, @Param max: Int) = s"regexNumber2 - min:$min - max:$max"

    @Get("regex:/mix/(?<min>\\d+)/(?<max>\\w+)")
    def regexMix1Endpoint(@Param min: Int, @Param max: String) = s"regexMix1-Int,String - min:$min - max:$max"
    
    @Get("regex:/mix/(?<min>\\w+)/(?<max>\\d+)")
    def regexMix2Endpoint(@Param min: String, @Param max: Int) = s"regexMix2-String,Int - min:$min - max:$max"
  }

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

}
