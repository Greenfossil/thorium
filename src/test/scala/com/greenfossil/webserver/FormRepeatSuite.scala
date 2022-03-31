package com.greenfossil.webserver

class FormRepeatSuite extends munit.FunSuite {

  import com.greenfossil.webserver.data.{*, given}

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
    assertNoDiff(form.apply("address").tpe, "C-Address")

    val bindedForm = form.bind(
      Map(
        "id" -> Seq("1"),
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
      "address" -> mappingRepeat[Address](
        "postalCode" -> text,
        "country" -> text
      )
    )

    assertNoDiff(form.apply("id").tpe, "Long")
    assertNoDiff(form.apply("address").tpe, "[C-Address")

    val bindedForm = form.bind(
      Map(
        "id" -> Seq("1"),
        "address[0].postalCode" -> "123456",
        "address[0].country" -> "Singapore"
      )
    )

    assertEquals(bindedForm.value, Some((1L, Seq(Address("123456", "Singapore")))))

  }

  test("bind tuple field".only) {
    val form: Form[(Long, (String, String))] = Form.tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text
      )
    )

    assertNoDiff(form.apply("id").tpe, "Long")
    assertNoDiff(form.apply("address").tpe, "C-")

    val bindedForm = form.bind(
      Map(
        "id" -> Seq("1"),
        "address.postalCode" -> "123456",
        "address.country" -> "Singapore"
      )
    )

    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore"))))

  }

  test("bind repeat tuple field".only) {
    val form: Form[(Long, Seq[(String, String)])] = Form.tuple(
      "id" -> longNumber,
      "address" -> tupleRepeat(
        "postalCode" -> text,
        "country" -> text
      )
    )

    assertNoDiff(form.apply("id").tpe, "Long")
    assertNoDiff(form.apply("address").tpe, "[C-")

    val bindedForm = form.bind(
      Map(
        "id" -> Seq("1"),
        "address[0].postalCode" -> "123456",
        "address[0].country" -> "Singapore"
      )
    )

    assertEquals(bindedForm.value, Some((1L, Seq(("123456", "Singapore")))))

  }

}
