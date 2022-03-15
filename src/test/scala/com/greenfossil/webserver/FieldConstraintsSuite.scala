package com.greenfossil.webserver

import com.greenfossil.webserver.data.*

class FieldConstraintsSuite extends munit.FunSuite {

  /*
   * For implementing constraints - use play.api.data.Forms for help. Need to use Constraints class
   */

  test("nonEmptyText") {
    val f = nonEmptyText
    val errorField = f.bind("")
    assertEquals(errorField.errors.size, 1)
    assertEquals(errorField.errors.head.message, "error.required")
    errorField.errors foreach println

    val errorField2 = f.bind(None)
    assertEquals(errorField2.errors.size, 1)
    assertEquals(errorField2.errors.head.message, "error.required")
    errorField2.errors foreach println

    val validField = f.bind("abc")
    assert(validField.errors.isEmpty)
  }

  test("email"){
    val f = email
    val errorField = f.bind("https://www.google.com")
    assertEquals(errorField.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val validField = f.bind("test@greenfossil.com")
    assert(validField.errors.isEmpty)
  }

  test("number"){
    val f = number(1, 10)

    val errorField = f.bind(20)
    assertEquals(errorField.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val validField = f.bind(5)
    assert(validField.errors.isEmpty)
  }

  test("text with trim option"){
    val f = text(1, 5, true)

    val errorField = f.bind("hello world")
    assertEquals(errorField.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val errorField2 = f.bind("    ")
    assertEquals(errorField2.errors.size, 1)
    errorField2.errors.foreach(e => println(e.message))
    assertEquals(errorField2.value, Option(""))

    val validField = f.bind("hello")
    assert(validField.errors.isEmpty)

    val validField2 = f.bind("hello ")
    assert(validField2.errors.isEmpty)
    assertEquals(validField2.value, Option("hello"))
  }

  test("text without trim option"){
    val f = text(1, 5, false)

    val errorField = f.bind("hello world")
    assertEquals(errorField.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val errorField2 = f.bind("hello ")
    assertEquals(errorField2.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val validField = f.bind("hello")
    assert(validField.errors.isEmpty)

    val validField2 = f.bind("    ")
    assert(validField.errors.isEmpty)
  }

  test("byte number"){
    val f = byteNumber(min = 2, max = 8)
    println(s"f.constraints = ${f.constraints}")

    val errorField = f.bind(10.toByte)
    assertEquals(errorField.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val validField = f.bind(8)
    assert(validField.errors.isEmpty)
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
    val field = text.verifying("text needs to be alphanumerical", _.matches("[a-zA-Z0-9]*"))

    val errorField = field.bind("@#$")
    assertEquals(errorField.errors.size, 1)
    assertEquals(errorField.errors.head.message, "text needs to be alphanumerical")

    val validField = field.bind("asdf1234")
    assert(validField.errors.isEmpty)
  }
}
