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
    val epParams = EndpointMcr(endpointParams(1)("string"))
    assertNoDiff(epParams.url, "/endpoint-params/1/string")
  }

  test("using"){
    given Request = null
    val epUsing = EndpointMcr(endpointUsing)
    assertNoDiff(epUsing.url, "/endpoint-using")
  }

}
