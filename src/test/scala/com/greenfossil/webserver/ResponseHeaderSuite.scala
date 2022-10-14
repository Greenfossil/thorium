package com.greenfossil.webserver

class ResponseHeaderSuite extends munit.FunSuite {

  test("ResponseHeader case insenitivity"){
    val header = ResponseHeader(Map("a" -> "apple", "b" -> "basket"))
    assertNoDiff(header.headers("a"), "apple")
    assertNoDiff(header.headers("A"), "apple")
  }

}
