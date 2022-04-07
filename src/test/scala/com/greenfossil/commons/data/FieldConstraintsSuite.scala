package com.greenfossil.commons.data

class FieldConstraintsSuite extends munit.FunSuite {

  /*
   * For implementing constraints - use play.api.data.Forms for help. Need to use Constraints class
   */

  test("nonEmptyText") {
    val f = nonEmptyText.name("f")
    val errorField = f.bind("f" -> "")
    assertEquals(errorField.errors.size, 1)
    assertEquals(errorField.errors.head.message, "error.required")

    val validField = f.bind("f" -> "abc")
    assertEquals(validField.errors, Nil)
  }

  test("email"){
    val f = email.name("f")
    val errorField = f.bind("f" -> "https://www.google.com")
    assertEquals(errorField.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val validField = f.bind("f" -> "test@greenfossil.com")
    assert(validField.errors.isEmpty)
  }

  test("number"){
    val f = number(1, 10).name("f")

    val errorField = f.bind("f" -> "20")
    assertEquals(errorField.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val validField = f.bind("f" -> "5")

    assert(validField.errors.isEmpty)
  }

  test("text with trim option"){
    val f = text(1, 5, true).name("f")

    val validField = f.bind("f" -> "hello")
    assert(validField.errors.isEmpty)

    val errorField = f.bind("f" -> "hello world")
    assertEquals(errorField.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val errorField2 = f.bind("f" -> "    ")
    assertEquals(errorField2.errors.size, 1)
    errorField2.errors.foreach(e => println(e.message))
    assertEquals(errorField2.value, Option(""))

    val validField2 = f.bind("f" -> "hello ")
    assert(validField2.errors.isEmpty)
    assertEquals(validField2.value, Option("hello"))
  }

  test("text without trim option"){
    val f = text(1, 5, false).name("f")

    val errorField = f.bind("f" -> "hello world")
    assertEquals(errorField.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val errorField2 = f.bind("f" -> "hello ")
    assertEquals(errorField2.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val validField = f.bind("f" -> "hello")
    assert(validField.errors.isEmpty)

    val validField2 = f.bind("f" -> "    ")
    assert(validField.errors.isEmpty)
  }

  test("byte number"){
    val f = byteNumber(min = 2, max = 8).name("f")
    println(s"f.constraints = ${f.constraints}")

    val errorField = f.bind("f" -> 10.toByte.toString)
    assertEquals(errorField.errors.size, 1)
    errorField.errors.foreach(e => println(e.message))

    val validField = f.bind("f" -> "8")
    assert(validField.errors.isEmpty)
  }

  test("short number"){
    val nonStrictField = shortNumber(1,10, false).name("f")

    val nonStrictValidField = nonStrictField.bind("f" -> "1")
    assertEquals(nonStrictValidField.errors.size, 0)

    val nonStrictErrorField = nonStrictField.bind("f" -> "0")
    assertEquals(nonStrictErrorField.errors.size, 1)
    assertEquals(nonStrictErrorField.errors.head.message, "error.min")

    val strictField = shortNumber(1,10, true).name("f")
    val strictValidField = strictField.bind("f" -> "2")
    assertEquals(strictValidField.errors.size, 0)

    val strictErrorField = strictField.bind("f" -> "0")
    assertEquals(strictErrorField.errors.size, 1)
    assertEquals(strictErrorField.errors.head.message, "error.min.strict")

  }

  test("long number"){
    val nonStrictField = longNumber(1,10, false).name("f")

    val nonStrictValidField = nonStrictField.bind("f" -> "1")
    assertEquals(nonStrictValidField.errors.size, 0)

    val nonStrictErrorField = nonStrictField.bind("f" -> "0")
    assertEquals(nonStrictErrorField.errors.size, 1)
    assertEquals(nonStrictErrorField.errors.head.message, "error.min")

    val strictField = longNumber(1,10, true).name("f")
    val strictValidField = strictField.bind("f" -> "2")
    assertEquals(strictValidField.errors.size, 0)

    val strictErrorField = strictField.bind("f" -> "0")
    assertEquals(strictErrorField.errors.size, 1)
    assertEquals(strictErrorField.errors.head.message, "error.min.strict")

  }

  test("big decimal"){
    val field = bigDecimal(1,1).name("f")
    val errorField = field.bind("f" -> "1") // scale is 0, precision is 1
    assertEquals(errorField.errors.size, 1)
    assertEquals(errorField.errors.head.message, "error.real.precision")

    val validField = field.bind("f" -> "0.1") // scale is 1, precision is 1
    assertEquals(validField.errors.size, 0)

  }

  test("custom constraint"){
    val field = text
      .name("f")
      .verifying("text needs to be alphanumerical", _.matches("[a-zA-Z0-9]*"))

    val errorField = field.bind("f" -> "@#$")
    assertEquals(errorField.errors.size, 1)
    assertEquals(errorField.errors.head.message, "text needs to be alphanumerical")

    val validField = field.bind("f" -> "asdf1234")
    assert(validField.errors.isEmpty)
  }
}
