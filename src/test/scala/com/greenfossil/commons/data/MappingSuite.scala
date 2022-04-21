package com.greenfossil.commons.data

class MappingSuite extends munit.FunSuite {
  
  test("verifying"){
    val f1 = Mapping.of[Int]("f1")
      .verifying("Int must be greater than 10", x => x > 10)
    val boundField = f1.bind("f1" -> "4")
    assertNoDiff(boundField.errors.head.messages.head , "Int must be greater than 10")
  }

  test("transform") {
    val f1 = Mapping.of[Int]("f1")
      .transform[Int](_ * 2)
      .verifying("X must be 8", x => x == 8)
    val boundField = f1.bind("f1" -> "4")
    assertEquals(boundField.value, Some(8))
    assert(boundField.errors.isEmpty)
  }

}
