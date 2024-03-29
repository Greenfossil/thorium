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

import com.greenfossil.thorium.*
import com.linecorp.armeria.server.annotation.{Get, Param, Path, Post}

import java.time.LocalDateTime

class EndpointMacro1Suite extends munit.FunSuite {

  @Get("/endpoint1")
  def endpoint1 = Action { _ => "endpoint1" }

  @Get("/endpoint2/:name")
  def endpoint2(@Param name : String) = Action { _ => "endpoint2"}

  @Get("/endpoint3") //query string
  def endpoint3(@Param name: String) = Action { _ => "endpoint3"}

  @Get("/endpoint4/:id")
  def endpoint4(@Param id: Long, @Param name: String) = Action { _ => "endpoint3"}

  @Post("/endpoint4/:name")
  def endpoint4(@Param members: Seq[String], @Param name: String, @Param time: LocalDateTime) = Action { _ => "endpoint5"}

  @Post
  @Path("/endpoint5/:id/:name")
  def endpoint5(@Param id: Long, @Param name: String) = Action { _ => "endpoint5" }

  test("annotated endpoint"){
    val ep1 = endpoint1.endpoint
    assertNoDiff(ep1.url, "/endpoint1")
    assertNoDiff(ep1.method, "Get")

    val ep2 = endpoint2("homer").endpoint
    assertNoDiff(ep2.url, "/endpoint2/homer")
    assertEquals(ep2.pathPatternOpt, Some("/endpoint2/:name"))
    assertNoDiff(ep2.method, "Get")

    val ep3 = endpoint3("homer").endpoint
    assertNoDiff(ep3.url, "/endpoint3?name=homer")
    assertNoDiff(ep3.method, "Get")
    assertEquals(ep3.queryParams.size, 1)

    val now = LocalDateTime.now
    val members = Seq("Marge Simpson", "Bart Simpson", "Maggie Simpson")
    val ep4 = endpoint4(members, "homer", now).endpoint
    assertNoDiff(ep4.url, "/endpoint4/homer?" +
        Endpoint.paramKeyValue("members", members) + "&" +
        Endpoint.paramKeyValue("time", now.toString))
    assertNoDiff(ep4.method, "Post")
    assertEquals(ep4.queryParams.size, 2)
  }

  test("url-encoded param Val string"){
    val valToken = "Homer/Marge Simpson"
    val url =  endpoint2(valToken).endpoint.url

    val url2 = endpoint2("Homer/Marge Simpson").endpoint.url

    assertNoDiff(url, url2)
    assertNoDiff(url, "/endpoint2/Homer%2FMarge%20Simpson")
  }

  test("url-encoded param val string (with path and query param)") {
    val valToken = "Homer/Marge Simpson"

    val url = endpoint4(1, valToken).endpoint.url

    val url2 = endpoint4(1, "Homer/Marge Simpson").endpoint.url

    assertNoDiff(url, url2)
    assertNoDiff(url, "/endpoint4/1?name=Homer%2FMarge%20Simpson")
  }

  test("url-encoded param val string (with query param)") {
    val valToken = "Homer/Marge Simpson"

    val url = endpoint3(valToken).endpoint.url

    val url2 = endpoint3("Homer/Marge Simpson").endpoint.url

    assertNoDiff(url, url2)
    assertNoDiff(url, "/endpoint3?name=Homer%2FMarge%20Simpson")
  }


  test("url-encoded param Def string") {
    def defToken = "Homer/Marge Simpson"
    val url = endpoint3(defToken).endpoint.url

    val url2 = endpoint3("Homer/Marge Simpson").endpoint.url

    assertNoDiff(url, url2)
    assertNoDiff(url, "/endpoint3?name=Homer%2FMarge%20Simpson")
  }

  test("endpoint with Path annotation"){
    val endpoint = endpoint5(1, "Homer").endpoint
    assertEquals(endpoint.url, "/endpoint5/1/Homer")
    assertEquals(endpoint.method, "Post")
  }

}
