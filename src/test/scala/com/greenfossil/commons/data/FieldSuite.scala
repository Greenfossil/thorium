package com.greenfossil.commons.data

class FieldSuite extends munit.FunSuite {
  
  test("verifying"){
    val f1 = Field.of[Int]("f1")
      .verifying("Int must be greater than 10", x => x > 10)
    assert(f1.binder != null)
    val boundField = f1.bind(Map("f1" -> "4"))
    assertNoDiff(boundField.errors.head.messages.head , "Int must be greater than 10")
  }
  
//  test("transform".fail) {
//    val f1 = Field.of[Int]("f1")
//      .transform[Int](_ * 2, _ * 2)
//      .verifying("X must be 8", x => x == 8)
//    val boundField = f1.bind(4)
//    assert(boundField.errors.isEmpty)
//  }

}
