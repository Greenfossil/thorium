package com.greenfossil.commons.data

/*
 * Test Tuple, CaseClass Field
 */
class FieldBind2Suite extends munit.FunSuite {

  test("Tuple") {
    val tupleField: Field[(String, Int)] = tuple(
      "f1" -> Field.of[String],
      "f2" -> Field.of[Int]
    ).name("tupleField")

    val boundField = tupleField.bind(Map("tupleField.f1" -> "Hello", "tupleField.f2" -> "1"))
    assertEquals(boundField.value, Some(("Hello", 1)))
  }

  test("Mapping") {
    case class Contact(name: String, number: Int)
    val tupleField: Field[Contact] = mapping[Contact](
      "name" -> Field.of[String],
      "number" -> Field.of[Int]
    ).name("contact")

    val boundField = tupleField.bind(Map("contact.name" -> "Hello", "contact.number" -> "1"))
    assertEquals(boundField.value, Some(Contact("Hello", 1)))
  }

}
