package com.greenfossil.webserver

import com.linecorp.armeria.server.annotation.{Get, Post, Path, Param}

class EndpointMacroSuite extends munit.FunSuite {

//  @Get("/endpoint1")
//  def endpoint1 = Action { req => "endpoint1" }
//
//  @Get("/endpoint2/:name")
//  def endpoint2(@Param name : String) = Action { req => "endpoint2"}

//  @Get("/endpoint3/{name}") //alternate variable notation
//  def endpoint3(@Param name : String) = Action { req => "endpoint3"}

//  @Get("/endpoint3") //query string
//  def endpoint3(@Param name: String) = Action { req => "endpoint3"}

@Get("/endpoint5/:wt")
def endpoint5(@Param wt: String,@Param name: String) = Action {req => "endpoint5"}

  test("annotated endpoint"){
//    val ep1 = Endpoint(endpoint1)
//    assertNoDiff(ep1.url, "/endpoint1")
//
//    val ep2 = Endpoint(endpoint2("homer"))
//    assertNoDiff(ep2.url, "/endpoint2/homer")
//
//    val ep3 = Endpoint(endpoint3("homer"))
//    assertNoDiff(ep3.url, "/endpoint3/homer")
//    val ep4 = Endpoint(endpoint4("homer"))

      val ep5 = Endpoint(endpoint5("heavy", "homer"))
    println(s"ep5 = ${ep5}")
  }
}
