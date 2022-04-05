package com.greenfossil.commons.data

class FormNestedFieldsSuite extends munit.FunSuite {

  test("bind case-class field") {
    case class Address(postalCode: String, country: String)
    val form: Form[(Long, Address)] = Form.tuple(
      "id" -> longNumber,
      "address" -> mapping[Address](
          "postalCode" -> text,
          "country" -> text
      )
    )

    assertNoDiff(form.apply("id").tpe, "Long")
    assertNoDiff(form.apply("address").tpe, "P+")

    val bindedForm = form.bind2(
      Map(
        "id" -> "1",
        "address.postalCode" -> "123456",
        "address.country" -> "Singapore"
      )
    )

    assertEquals(bindedForm.value, Some((1L, Address("123456", "Singapore"))))

  }

  test("bind repeating case class field") {
    case class Address(postalCode: String, country: String)
    val form: Form[(Long, Seq[Address])] = Form.tuple(
      "id" -> longNumber,
      "address" -> repeatedMapping[Address](
        "postalCode" -> text,
        "country" -> text
      )
    )

    assertNoDiff(form.apply("id").tpe, "Long")
    assertNoDiff(form.apply("address").tpe, "[")

    val bindedForm = form.bind2(
      Map(
        "id" -> "1",
        "address[0].postalCode" -> "123456",
        "address[0].country" -> "Singapore"
      )
    )

    assertEquals(bindedForm.value, Some((1L, Seq(Address("123456", "Singapore")))))

  }

  test("bind tuple field") {
    val form: Form[(Long, (String, String))] = Form.tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text
      )
    )

    assertNoDiff(form.apply("id").tpe, "Long")
    assertNoDiff(form.apply("address").tpe, "P-")

    val bindedForm = form.bind2(
      Map(
        "id" -> "1",
        "address.postalCode" -> "123456",
        "address.country" -> "Singapore"
      )
    )

    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore"))))

  }

  test("bind repeat tuple field") {
    val form: Form[(Long, Seq[(String, String)])] = Form.tuple(
      "id" -> longNumber,
      "address" -> repeatedTuple(
        "postalCode" -> text,
        "country" -> text
      )
    )

    assertNoDiff(form.apply("id").tpe, "Long")
    assertNoDiff(form.apply("address").tpe, "[")

    val bindedForm = form.bind2(
      Map(
        "id" -> "1",
        "address[0].postalCode" -> "123456",
        "address[0].country" -> "Singapore"
      )
    )

    assertEquals(bindedForm.value, Some((1L, Seq(("123456", "Singapore")))))

  }

}
