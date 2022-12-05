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

import com.linecorp.armeria.server.annotation.{Get, Param}

class RegexEndpointSuite extends munit.FunSuite{
  object ParameterizedServices {
    @Get("regex:^/ci/(?<name>(?i)Homer|Marge)")
    def caseInsensitivity(@Param name: String) = name

    @Get("regex:(?i)^/ci2/(?<name>Homer|Marge)")
    def caseInsensitivity2(@Param name: String) = name

    @Get("regex:^/(?i)ci3(?-i)/(?<name>Homer|Marge)")
    def caseInsensitivity3(@Param name: String) = name

    @Get("regex:^/ci4/(?<name>(?i)Homer|Marge(?-i))")
    def caseInsensitivity4(@Param name: String) = name
  }

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
