package com.greenfossil.webserver

import com.greenfossil.webserver.data.*

class FieldConstraintsSuite extends munit.FunSuite {
  
  test("nonEmptyText") {
    val f = nonEmptyText
    val errorField = f.bind("")
    assertEquals(errorField.errors.size, 1)
    assertEquals(errorField.errors.head.message, "error.required")
    errorField.errors foreach println

    val validField = f.bind("abc")
    assert(validField.errors.isEmpty)
  }

  test("email"){

  }

  test("number"){

  }

  test("text"){

  }

  test("byte number"){

  }

  test("short number"){
    val nonStrictField = shortNumber(1,10, false)
    val nonStrictErrorField = nonStrictField.bind("0")
    assertEquals(nonStrictErrorField.errors.size, 1)
    assertEquals(nonStrictErrorField.errors.head.message, "error.min")

    val strictField = shortNumber(1,10, true)
    val strictErrorField = strictField.bind("0")
    assertEquals(strictErrorField.errors.size, 1)
    assertEquals(strictErrorField.errors.head.message, "error.min.strict")

    val nonStrictValidField = nonStrictField.bind("1")
    assertEquals(nonStrictValidField.errors.size, 0)

    val strictValidField = strictField.bind("2")
    assertEquals(strictValidField.errors.size, 0)
  }

  test("long number"){
    val nonStrictField = longNumber(1,10, false)
    val nonStrictErrorField = nonStrictField.bind("0")
    assertEquals(nonStrictErrorField.errors.size, 1)
    assertEquals(nonStrictErrorField.errors.head.message, "error.min")

    val strictField = longNumber(1,10, true)
    val strictErrorField = strictField.bind("0")
    assertEquals(strictErrorField.errors.size, 1)
    assertEquals(strictErrorField.errors.head.message, "error.min.strict")

    val nonStrictValidField = nonStrictField.bind("1")
    assertEquals(nonStrictValidField.errors.size, 0)

    val strictValidField = strictField.bind("2")
    assertEquals(strictValidField.errors.size, 0)
  }

  test("big decimal"){
    val field = bigDecimal(1,1)
    val errorField = field.bind("1") // scale is 0, precision is 1
    assertEquals(errorField.errors.size, 1)
    assertEquals(errorField.errors.head.message, "error.real.precision")

    val validField = field.bind("0.1") // scale is 1, precision is 1
    assertEquals(validField.errors.size, 0)

  }

  test("custom constraint"){

  }
}
