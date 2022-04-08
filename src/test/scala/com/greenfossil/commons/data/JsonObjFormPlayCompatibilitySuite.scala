package com.greenfossil.commons.data

import com.greenfossil.commons.json.Json

class JsonObjFormPlayCompatibilitySuite extends munit.FunSuite {

  /*
   * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"s":"my_login","xs":"my_password"}' -o /dev/null
   */
  test("valid default value") {
    val form: Form[(String, Boolean)] = Form.tuple(
      "text" -> default(text, "Foo"),
      "isChecked" -> checked("this should be checked")
    )
    val jsonObject = Json.obj(
      "text" -> "chicken", "isChecked" -> true,
    )

    val filledForm = form.bind(jsonObject)
    assertEquals[Any, Any](filledForm.data.get("text"), Option("chicken"))
    assertEquals[Any, Any](filledForm.data.get("isChecked"), Option(true))
  }

  test("empty default value") {
    val form: Form[(String, Boolean)] = Form.tuple(
      "text" -> default(text, "Foo"),
      "isChecked" -> checked("this should be checked")
    )

    val jsonObject = Json.obj(
      "isChecked" -> true,
    )
    val filledForm = form.bind(jsonObject)
    assertEquals[Any, Any](filledForm.data.get("text"), Option("Foo"))
    assertEquals[Any, Any](filledForm.data.get("isChecked"), Option(true))
  }

  /*
  * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq[1]":"1","seq[2]":"2","seq[3]":"3"}' -o /dev/null
  */
  test("repeated with valid index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(
      "seq[1]" -> "1",
      "seq[2]" -> "2",
      "seq[3]" -> "3",
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq[1]":"1","seq[1]":"2","seq[1]":"3"}' -o /dev/null
  */
  test("repeated with same index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(
      "seq[1]" -> "1",
      "seq[1]" -> "2",
      "seq[1]" -> "3",
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(1)))
  }

  /*
   * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq[3]":"1","seq[2]":"2","seq[1]":"3"}' -o /dev/null
   */
  test("repeated with inverted index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(
        "seq[3]" -> "1",
        "seq[2]" -> "2",
        "seq[1]" -> "3",

    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(3, 2, 1)))
  }
  /*
   * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq[1]":"1","seq[2]":"2","seq[4]":"3"}' -o /dev/null
   */
  test("repeated with gap in index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(

        "seq[1]" -> "1",
        "seq[2]" -> "2",
        "seq[4]" -> "3",

    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }
  /*
  * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq[]":"1","seq[]":"2","seq[]":"3"}' -o /dev/null
  */
  test("repeated with no index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(

        "seq[]" -> "1",
        "seq[]" -> "2",
        "seq[]" -> "3",

    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq[-1]":"1","seq[0]":"2","seq[1]":"3"}' -o /dev/null
  */
  test("repeated with negative value as an index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(
        "seq[-1]" -> "1",
        "seq[0]" -> "2",
        "seq[1]" -> "3",
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(2, 3)))
  }

  /*
   * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq[1]":"1","seq[]":"2","seq[3]":"3"}' -o /dev/null
   */
  test("repeated with 1 empty index []") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(
      "seq[1]" -> "1",
      "seq[]" -> "2",
      "seq[3]" -> "3",
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(2, 1, 3)))
  }

  /*
   * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq[1]":"1","seq[]":"2","seq[]":"3"}' -o /dev/null
   */
  test("repeated with 2 empty index [] ") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(
      "seq[1]" -> "1",
      "seq[]" -> "2",
      "seq[]" -> "3",
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(2, 3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq[1]":"1","seq[]":"2","seq[]":"2","seq[2]":"3"}' -o /dev/null
  */
  test("repeated with same value with empty index [] ") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(
      "seq[1]" -> "1",
      "seq[]" -> "2",
      "seq[]" -> "2",
      "seq[2]" -> "3",
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(2, 1, 3)))
  }

  /*
  * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq[1]":"1","seq[]":"2","seq[]":"5","seq[2]":"3"}' -o /dev/null
  */
  test("repeated with different value with empty index [] ") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(
      "seq[1]" -> "1",
      "seq[]" -> "2",
      "seq[]" -> "5",
      "seq[2]" -> "3",
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(5, 1, 3)))
  }

  /*
   * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"id":"1","address.postalCode":"123456","address.country":"Singapore"}' -o /dev/null
   */
  test("nested field") {
    val form: Form[(Long, (String, String))] = Form.tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text
      )
    )

    val jsonObject = Json.obj(
      "id" -> "1",
      "address.postalCode" -> "123456",
      "address.country" -> "Singapore"
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore"))))
  }

  /*
   * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"id":"1","address.postalCode":"123456","address.country":"Singapore","address.numList.num":"1","address.numList.num2":"2"}' -o /dev/null
   */
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

    val jsonObject = Json.obj(
      "id" -> "1",
      "address.postalCode" -> "123456",
      "address.country" -> "Singapore",
      "address.numList.num" -> "1",
      "address.numList.num2" -> "2",
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore", (1L, 2L)))))
  }

  test("binded ignored"){
    val form = Form("s", ignored("Foo"))
    val data = "s" -> "Bar"

    val bindedForm = form.bind(data)
    assertEquals(bindedForm.value, Some("Foo"))
  }

  test("filled ignored"){
    val form: Form[String] = Form("s", ignored("Foo"))
    val bindedForm = form.fill("Bar")
    assertEquals(bindedForm.value, Some("Foo"))
  }

}
