package com.greenfossil.thorium

import com.linecorp.armeria.server.annotation.{Get, Param}

class EndpointMacro2Suite extends munit.FunSuite {

  @Get("/endpoint1/:int/:s")
  def endpoint1(@Param int: Int)(@Param s: String)=
    "endpoint1"

  @Get("/endpoint/:int")
  def endpoint(int: Int)(using request: Request) =
    s"endpoint Int:${int}"


  test("annotated endpoint") {
    val ep1 = endpoint1(1)("string").url
    println(s"ep1 = ${ep1}")

  }

}
