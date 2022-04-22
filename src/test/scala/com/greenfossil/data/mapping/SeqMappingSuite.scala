package com.greenfossil.data.mapping

class SeqMappingSuite extends munit.FunSuite {
  import Mapping.*

  test("repeated with no []") {
    val form: Mapping[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq" -> "1",
      "seq" -> "2",
      "seq" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with path with no []") {
    val form: Mapping[(String, Seq[String])] =
      tuple(
        "maincourse" -> text,
        "drinks" -> seq[String]
      ).name("food")

    val boundForm = form.bind(
      "food.maincourse" -> "pizza",
      "food.drinks[1]" -> "coke",
      "food.drinks[2]" -> "pepsi"
    )
    assertEquals(boundForm.value, Some(("pizza", Seq("coke", "pepsi"))))
  }

  test("repeated tuples") {
    val form: Mapping[Seq[(Int, Int)]] =
      repeatedTuple(
        "num1"-> number,
        "num2" -> number
      ).name("seq")


    val boundForm = form.bind(
      "seq[0].num1" -> "1",
      "seq[0].num2" -> "2",
      "seq[1].num1" -> "11",
      "seq[1].num2" -> "22",
    )
    assertEquals(boundForm.value, Some(Seq((1, 2), (11, 22))))
  }

  test("repeated with same index []") {
    val form: Mapping[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[1]" -> "1",
      "seq[1]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with empty index []") {
    val form: Mapping[Seq[Int]] = seq[Int].name("seq")

    val boundForm = form.bind(
      "seq[1]" -> "1",
      "seq[]" -> "5",
      "seq[]" -> "2",
      "seq[2]" -> "3",
    )
    assertEquals(boundForm.value, Some(Seq(5, 2, 1, 3)))
  }

}
