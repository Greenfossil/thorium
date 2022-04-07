package com.greenfossil.commons.data

class FieldBind4Suite extends munit.FunSuite {

  test("Seq[Int]") {
    val seqIntField = seq[Int].name("i")
    val boundField = seqIntField.bind("i[1]" -> "1", "i[2]" -> "2", "i[3]" -> "3")
    assertEquals(boundField.value, Some(Seq(1, 2, 3)))
  }

  test("Seq[String]") {
    val seqIntField = seq[String].name("s")
    val boundField = seqIntField.bind("s[1]" -> "1", "s[2]" -> "2", "s[3]" -> "3")
    assertEquals(boundField.value, Some(Seq("1", "2", "3")))
  }

  test("Seq[Tuple]") {
    val tupleField: Field[Seq[(String, Int)]] = repeatedTuple(
      "name" -> Field.of[String],
      "number" -> Field.of[Int]
    ).name("contact")

    val boundField = tupleField.bind(
      "contact[1].name" -> "Homer",
      "contact[1].number" -> "123",
      "contact[2].name" -> "Marge",
      "contact[2].number" -> "456"
    )
    assertEquals(boundField.value, Some(Seq(("Homer", 123), ("Marge", 456))))
  }

  test("Seq[Mapping]") {
    case class Contact(name: String, number: Int)
    val tupleField: Field[Seq[Contact]] = repeatedMapping[Contact](
      "name" -> Field.of[String],
      "number" -> Field.of[Int]
    ).name("contact")

    val boundField = tupleField.bind(
      "contact[1].name" -> "Homer",
      "contact[1].number" -> "123",
      "contact[2].name" -> "Marge",
      "contact[2].number" -> "456"
    )
    assertEquals(boundField.value, Some(Seq(Contact("Homer", 123), Contact("Marge", 456))))
  }

}
