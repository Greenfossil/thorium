package com.greenfossil.commons.data

/*
 * Test Tuple, CaseClass Field
 */
class MappingBind2Suite extends munit.FunSuite {

  test("Tuple") {
    val tupleField: Mapping[(String, Int)] = tuple(
      "f1" -> Mapping.of[String],
      "f2" -> Mapping.of[Int]
    ).name("tupleField")

    val boundField = tupleField.bind("tupleField.f1" -> "Hello", "tupleField.f2" -> "1")
    assertEquals(boundField.value, Some(("Hello", 1)))
  }

  test("Mapping") {
    case class Contact(name: String, number: Int)
    val tupleField: Mapping[Contact] = mapping[Contact](
      "name" -> Mapping.of[String],
      "number" -> Mapping.of[Int]
    ).name("contact")

    val boundField = tupleField.bind("contact.name" -> "Hello", "contact.number" -> "1")
    assertEquals(boundField.value, Some(Contact("Hello", 1)))
  }

}
