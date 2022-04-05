package com.greenfossil.commons.data

/*
 * Test for Option and Seq Field
 */
class FieldBind3Suite extends munit.FunSuite {

  test("Option[Int]") {
    val optIntField: OptionalField[Int] =  optional[Int].name("optInt")
    optIntField.fill(None)
    val boundField = optIntField.bind(Map("optInt" -> "1"))
    assertEquals(boundField.value, Option(1))
  }

  test("Option[String]") {
    val optIntField = optional[String].name("optString")
    val boundField = optIntField.bind(Map("optString" -> "Hello World!"))
    assertEquals(boundField.value, Option("Hello World!"))
  }

  test("Option[Tuple]") {
    val tupleField: OptionalField[(String, Int)] = optionalTuple(
      "name" -> Field.of[String],
      "contact" -> Field.of[Int]
    ).name("tupleField")

    val boundField = tupleField.bind(Map("tupleField.name" -> "Hello World!", "tupleField.contact" -> "123"))
    assertEquals(boundField.value, Option(("Hello World!", 123)))
  }

  test("Option[Mapping]") {
    case class Contact(name: String, contact: Int)
    val tupleField: OptionalField[Contact] = optionalMapping[Contact](
      "name" -> Field.of[String],
      "contact" -> Field.of[Int]
    ).name("tupleField")

    val boundField = tupleField.bind(Map("tupleField.name" -> "Hello World!", "tupleField.contact" -> "123"))
    assertEquals(boundField.value, Option(Contact("Hello World!", 123)))
  }

}
