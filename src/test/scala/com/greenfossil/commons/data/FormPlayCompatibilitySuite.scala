package com.greenfossil.commons.data

class FormPlayCompatibilitySuite extends munit.FunSuite {

  /*
   * Html form-url-encoded
   */
  test("valid default value") {
    val form: Form[(String, Boolean)] = Form.tuple(
      "s" -> default(text, "Foo"),
      "xs" -> checked("this should be checked")
    )

    val filledForm = form.bind(
      "s" -> "chicken",
      "xs" -> "true"
    )

    filledForm.fold(
      errorForm => fail("should not have errors"),
      data => {
        println(s"data = ${data}")
        assertEquals(data, ("chicken", true))
      }
    )
  }

  test("empty default value") {
    val form: Form[(String, Boolean)] = Form.tuple(
      "s" -> default(text, "Foo"),
      "xs" -> checked("this should be checked")
    )

    val filledForm = form.bind("xs" -> "true")
    filledForm.fold(
      errorForm => fail("should not have errors"),
      data => {
        println(s"data = ${data}")
        assertEquals(data, ("Foo", true))
      }
    )
  }

  test("repeated with valid index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[2]" -> "2",
      "seq[3]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with same index []".fail) { //Will not be compatible
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[1]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(1)))
  }

  test("repeated with inverted index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[3]" -> "1",
      "seq[2]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(3, 2, 1)))
  }

  test("repeated with gap in index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[2]" -> "2",
      "seq[4]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with no index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[]" -> "1",
      "seq[]" -> "2",
      "seq[]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with 0 as an index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[0]" -> "1",
      "seq[1]" -> "2",
      "seq[2]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with negative value as an index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[-1]" -> "1",
      "seq[0]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(2, 3)))
  }

  test("repeated with 1 empty index []".fail) {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[]" -> "2",
      "seq[3]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(2, 1, 3)))
  }

  test("repeated with 2 empty index [] ".fail) {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[]" -> "2",
      "seq[]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(2, 3)))
  }

  test("nested field") {
    val form: Form[(Long, (String, String))] = Form.tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text
      )
    )

    val bindedForm = form.bind(
      "id" -> "1",
      "address.postalCode" -> "123456",
      "address.country" -> "Singapore"
    )
    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore"))))
  }

  test("double nested field".only) {
    val form: Form[(Long, (String, String, (Long, Long, (String, String))))] = Form.tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text,
        "numList" -> tuple(
          "num" -> longNumber,
          "num2" -> longNumber,
          "member" -> tuple(
            "name1" -> text,
            "name2" -> text
          )
        )
      )
    )

    val bindedForm = form.bind(
      "id" -> "1",
      "address.postalCode" -> "123456",
      "address.country" -> "Singapore",
      "address.numList.num" -> "1",
      "address.numList.num2" -> "2",
      "address.numList.member.name1" -> "John",
      "address.numList.member.name2" -> "Doe",
    )
    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore", (1L, 2L, ("John", "Doe"))))))
  }

  test("binded ignored"){
    val form = Form("s", ignored("Foo"))
    val bindedForm = form.bind("s" -> "Bar")
    assertEquals(bindedForm.value, Some("Foo"))
  }

  test("filled ignored"){
    val form: Form[String] = Form("s", ignored("Foo"))
    val bindedForm = form.fill("Bar")
    assertEquals(bindedForm.value, Some("Foo"))
  }

}
