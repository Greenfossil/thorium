package com.greenfossil.webserver

import com.linecorp.armeria.server.annotation.{Get, Param, Path, Post}

import java.time.LocalDateTime

class EndpointMacroSuite extends munit.FunSuite {

  @Get("/endpoint1")
  def endpoint1 = Action { req => "endpoint1" }

  @Get("/endpoint2/:name")
  def endpoint2(@Param name : String) = Action { req => "endpoint2"}

  @Get("/endpoint3") //query string
  def endpoint3(@Param name: String) = Action { req => "endpoint3"}

  @Post("/endpoint4/:name")
  def endpoint4(@Param members: Seq[String], @Param name: String, @Param time: LocalDateTime) = Action { req => "endpoint5"}

  test("annotated endpoint"){
    val ep1 = Endpoint(endpoint1)
    assertNoDiff(ep1.url, "/endpoint1")
    assertNoDiff(ep1.method, "Get")

    val ep2 = Endpoint(endpoint2("homer"))
    assertNoDiff(ep2.url, "/endpoint2/homer")
    assertNoDiff(ep2.method, "Get")

    val ep3 = Endpoint(endpoint3("homer"))
    assertNoDiff(ep3.url, "/endpoint3?name=homer")
    assertNoDiff(ep3.method, "Get")
    assertEquals(ep3.queryParams.size, 1)

    val now = LocalDateTime.now
    val members = Seq("Marge Simpson", "Bart Simpson", "Maggie Simpson")
    val ep4 = Endpoint(endpoint4(members, "homer", now))
    assertNoDiff(ep4.url, "/endpoint4/homer?" +
        Endpoint.paramKeyValue("members", members) + "&" +
        Endpoint.paramKeyValue("time", now.toString))
    assertNoDiff(ep4.method, "Post")
    assertEquals(ep4.queryParams.size, 2)
  }
}
