package com.greenfossil.commons.data

class SeqFieldSuite extends munit.FunSuite {

  test("repeated with no []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val boundForm = form.bind(
      "seq" -> "1",
      "seq" -> "2",
      "seq" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with path with no []") {
    val form: Form[(String, Seq[String])] = Form(
      "food", tuple(
        "maincourse" -> text,
        "drinks" -> seq[String]
      )
    )
    val boundForm = form.bind(
      "food.maincourse" -> "pizza",
      "food.drinks[1]" -> "coke",
      "food.drinks[2]" -> "pepsi"
    )
    assertEquals(boundForm.value, Some(("pizza", Seq("coke", "pepsi"))))
  }

  test("repeated tuples") {
    val form: Form[Seq[(Int, Int)]] = Form(
      "seq", repeatedTuple(
        "num1"-> number,
        "num2" -> number
      )
    )

    val boundForm = form.bind(
      "seq[0].num1" -> "1",
      "seq[0].num2" -> "2",
      "seq[1].num1" -> "11",
      "seq[1].num2" -> "22",
    )
    assertEquals(boundForm.value, Some(Seq((1, 2), (11, 22))))
  }

  test("repeated with same index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val boundForm = form.bind(
      "seq[1]" -> "1",
      "seq[1]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with empty index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val boundForm = form.bind(
      "seq[1]" -> "1",
      "seq[]" -> "5",
      "seq[]" -> "2",
      "seq[2]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(5, 2, 1, 3)))
  }

}
