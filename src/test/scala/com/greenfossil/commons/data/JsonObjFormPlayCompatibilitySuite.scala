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
  * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"seq": ["1","2""3"]}' -o /dev/null
  */
  test("seq") {
    val form: Form[Seq[Int]] = Form("seq", seq[Int])

    val jsonObject = Json.obj(
      "seq" -> Seq("1", "2","3")
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some(Seq(1, 2, 3)))
  }

  /*
   * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"id":1,"address":{"postalCode":"123456","country":"Singapore"}}' -o /dev/null
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
      "id" -> 1,
      "address" -> Json.obj(
        "postalCode" -> "123456",
        "country" -> "Singapore"
      )
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore"))))
  }

  /*
   * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"id":"1","address":{"postalCode":"123456","country":"Singapore","numList":{"num":1,"num2":2}}}' -o /dev/null
   */
  test("double nested tuple fields") {
    val form: Form[(Long, (String, String, (Long, Long)))] = Form.tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text,
        "numList" -> tuple(
          "num1" -> longNumber,
          "num2" -> longNumber,
        )
      )
    )

    val jsonObject = Json.obj(
      "id" -> "1",
      "address" -> Json.obj(
        "postalCode" -> "123456",
        "country" -> "Singapore",
        "numList" -> Json.obj(
          "num1" -> 1,
          "num2" -> 2)
      )
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore", (1L, 2L)))))
  }

  /*
 * curl http://localhost:9000/form -X POST -H 'Content-Type: application/json' -d '{"id":"1","address":{"postalCode":"123456","country":"Singapore","numList":[1,2]}}' -o /dev/null
 */
  test("double nested tuple fields with seq field") {
    val form: Form[(Long, (String, String, Seq[Long]))] = Form.tuple(
      "id" -> longNumber,
      "address" -> tuple(
        "postalCode" -> text,
        "country" -> text,
        "numList" -> seq[Long]
      )
    )

    val jsonObject = Json.obj(
      "id" -> "1",
      "address" -> Json.obj(
        "postalCode" -> "123456",
        "country" -> "Singapore",
        "numList" -> Json.arr(1,2)
      )
    )

    val bindedForm = form.bind(jsonObject)
    assertEquals(bindedForm.value, Some((1L, ("123456", "Singapore", Seq(1L, 2L)))))
  }

}
