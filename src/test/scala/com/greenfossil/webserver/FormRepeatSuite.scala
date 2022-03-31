package com.greenfossil.webserver

class FormRepeatSuite extends munit.FunSuite {

  import com.greenfossil.webserver.data.{*, given}

  test("bind CaseClass field") {
    case class Address(postalCode: String, country: String)
    val form: Form[(Long, Address)] = Form.tuple(
      "id" -> longNumber,
      "address" -> mapping[Address](
          "postalCode" -> text,
          "country" -> text
      )
    )

    form.mappings.toList.map(_.asInstanceOf[Field[?]]) foreach { f =>
      println("Field type" + f.tpe)
    }

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

  test("bind Seq[CaseClass]") {
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

    println(s"bindedForm.value = ${bindedForm.value}")

    assertEquals(bindedForm.value, Some((1L, Seq(Address("123456", "Singapore")))))

  }

}
