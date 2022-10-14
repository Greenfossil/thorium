package com.greenfossil.thorium

import com.greenfossil.thorium.{*, given}

class ResponseHeaderSuite extends munit.FunSuite {

  test("ResponseHeader case insenitivity"){
    val header = ResponseHeader(Map("a" -> "apple", "b" -> "basket"))
    assertNoDiff(header.headers("a"), "apple")
    assertNoDiff(header.headers("A"), "apple")
  }

}
