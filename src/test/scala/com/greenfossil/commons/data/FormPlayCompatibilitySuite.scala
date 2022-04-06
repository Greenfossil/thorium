package com.greenfossil.commons.data

class FormPlayCompatibilitySuite extends munit.FunSuite {

  test("valid default value") {
    val form: Form[(String, Boolean)] = Form.tuple(
      "s" -> default(text, "Foo"),
      "xs" -> checked("this should be checked")
    )
    val data = Map(
      "s" -> "chicken",
      "xs" -> "true",
    )

    val filledForm = form.bind(data)
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
    val data = Map(
      "xs" -> "true",
    )

    val filledForm = form.bind(data)
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
      Map(
        "seq[1]" -> "1",
        "seq[2]" -> "2",
        "seq[3]" -> "3",
      )
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with same index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      Map(
        "seq[1]" -> "1",
        "seq[1]" -> "2",
        "seq[1]" -> "3",
      )
    )
    assertEquals(bindedForm.value, Some(Seq(1)))
  }

  test("repeated with inverted index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      Map(
        "seq[3]" -> "1",
        "seq[2]" -> "2",
        "seq[1]" -> "3",
      )
    )
    assertEquals(bindedForm.value, Some(Seq(3, 2, 1)))
  }

  test("repeated with gap in index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      Map(
        "seq[1]" -> "1",
        "seq[2]" -> "2",
        "seq[4]" -> "3",
      )
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with no index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      Map(
        "seq[]" -> "1",
        "seq[]" -> "2",
        "seq[]" -> "3",
      )
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with 0 as an index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      Map(
        "seq[0]" -> "1",
        "seq[1]" -> "2",
        "seq[2]" -> "3",
      )
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with negative value as an index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      Map(
        "seq[-1]" -> "1",
        "seq[0]" -> "2",
        "seq[1]" -> "3",
      )
    )
    assertEquals(bindedForm.value, Some(Seq(2, 3)))
  }

  test("repeated with 1 empty index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      Map(
        "seq[1]" -> "1",
        "seq[]" -> "2",
        "seq[3]" -> "3",
      )
    )
    assertEquals(bindedForm.value, Some(Seq(2, 1, 3)))
  }

  test("repeated with 2 empty index [] ") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      Map(
        "seq[1]" -> "1",
        "seq[]" -> "2",
        "seq[]" -> "3",
      )
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
      Map(
        "id" -> "1",
        "address.postalCode" -> "123456",
        "address.country" -> "Singapore"
      )
    )
    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore"))))
  }

  test("double nested field") {
    val form: Form[(Long, (String, String, (Long, Long)))] = Form.tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text,
        "numList" -> tuple(
          "num" -> longNumber,
          "num2" -> longNumber,
        )
      )
    )

    val bindedForm = form.bind(
      Map(
        "id" -> "1",
        "address.postalCode" -> "123456",
        "address.country" -> "Singapore",
        "address.numList.num" -> "1",
        "address.numList.num" -> "2",
      )
    )
    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore", (1L, 2L)))))
  }

  //  test("ignored"){
  //    val form: Form[String] = Form("s", ignored(text, "Foo"))
  //    val data = Map(
  //      "s" -> "Bar"
  //    )
  //    val bindedForm = form.bind(data)
  //    assertEquals(bindedForm.value, Some("Foo"))
  //  }

}
