package com.greenfossil.commons.data

import com.greenfossil.commons.json.Json

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

  /*
  * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[2]=2' -d 'seq[3]=3' -o /dev/null
  */
  test("repeated with valid index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[2]" -> "2",
      "seq[3]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[1]=2' -d 'seq[1]=3' -o /dev/null
  */
  test("repeated with same index []".fail) { //Will not be compatible
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[1]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(1)))
  }

  /*
   * curl http://localhost:9000/form -X POST -d 'seq[3]=1' -d 'seq[2]=2' -d 'seq[1]=3' -o /dev/null
   */
  test("repeated with inverted index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[3]" -> "1",
      "seq[2]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(3, 2, 1)))
  }
  /*
   * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[2]=2' -d 'seq[4]=3' -o /dev/null
   */
  test("repeated with gap in index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[2]" -> "2",
      "seq[4]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }
  /*
  * curl http://localhost:9000/form -X POST -d 'seq[]=1' -d 'seq[]=2' -d 'seq[]=3' -o /dev/null
  */
  test("repeated with no index []".only) {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[1]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }


  test("repeated with no []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[]" -> "1",
      "seq[]" -> "2",
      "seq" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  test("repeated with path with no []") {
    val form: Form[(String, Seq[String])] = Form(
      "food", tuple(
        "maincourse" -> text,
        "drinks" -> seq[String]
      )
    )
    val bindedForm = form.bind(
     "food.maincourse" -> "pizza",
      "food.drinks[1]" -> "coke",
      "food.drinks[2]" -> "pepsi"
    )
    assertEquals(bindedForm.value, Some(("pizza", Seq("coke", "pepsi"))))
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

  test("repeated tuples") {
    val form: Form[Seq[(Int, Int)]] = Form(
      "seq", repeatedTuple(
        "num1"-> number,
        "num2" -> number
      )
    )

    val bindedForm = form.bind(
      "seq[0].num1" -> "1",
      "seq[0].num2" -> "2",
      "seq[1].num1" -> "11",
      "seq[1].num2" -> "22",
    )
    assertEquals(bindedForm.value, Some(Seq((1, 2), (11, 22))))
  }

  /*
  * curl http://localhost:9000/form -X POST -d 'seq[-1]=1' -d 'seq[0]=2' -d 'seq[1]=3' -o /dev/null
  */
  test("repeated with negative value as an index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[-1]" -> "1",
      "seq[0]" -> "2",
      "seq[1]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(2, 3)))
  }

  /*
   * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[]=2' -d 'seq[3]=3' -o /dev/null
   */
  test("repeated with 1 empty index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[]" -> "2",
      "seq[3]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(2, 1, 3)))
  }

  /*
   * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[]=2' -d 'seq[]=3' -o /dev/null
   */
  test("repeated with 2 empty index [] ".fail) {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
      "seq[1]" -> "1",
      "seq[]" -> "2",
      "seq[]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(2, 3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[]=2' -d 'seq[]=2' -d 'seq[2]=3' -o /dev/null
  */
  test("repeated with same value with empty index [] ".fail) {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
        "seq[1]" -> "1",
        "seq[]" -> "2",
        "seq[]" -> "2",
        "seq[2]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(2, 1, 3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -d 'seq[1]=1' -d 'seq[]=2' -d 'seq[]=5' -d 'seq[2]=3' -o /dev/null
  */
  test("repeated with different value with empty index [] ".fail) {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val bindedForm = form.bind(
        "seq[1]" -> "1",
        "seq[]" -> "2",
        "seq[]" -> "5",
        "seq[2]" -> "3",
    )
    assertEquals(bindedForm.value, Some(Seq(2, 5, 3)))
  }

  /*
   * curl http://localhost:9000/form -X POST -d 'id=1' -d 'address.postalCode="123456"' -d 'address.country="Singapore"' -o /dev/null
   */
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

  /*
   * curl http://localhost:9000/form -X POST -d 'id=1' -d 'address.postalCode="123456"' -d 'address.country="Singapore"' -d 'address.numList.num=1' -d 'address.numList.num2=2' -o /dev/null
   */
  test("double nested field") {
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
