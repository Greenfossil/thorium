package com.greenfossil.commons.data

import com.greenfossil.commons.json.Json

import java.time.LocalDate

class FormFillSuite extends munit.FunSuite {

  test("tuple 2") {
    val form: Form[(Long, String)] = Form.tuple(
      "long" -> longNumber,
      "text" -> text
    )
    val filledForm = form.fill(1, "hello")
    val f: Field[Long] = filledForm("long")
    assertEquals[Any, Any](filledForm("long").value, Option(1))
    assertEquals[Any, Any](filledForm("text").value, Option("hello"))

    assertEquals[Any, Any](filledForm.data.get("long"), Option(1))
    assertEquals[Any, Any](filledForm.data.get("text"), Option("hello"))

    assertEquals(filledForm.value, Some((1L, "hello")))
  }

  test("fill tuple") {
    val form: Form[(Long, String, Seq[Long])] = Form.tuple(
      "long" -> longNumber,
      "text" -> text,
      "seq" -> seq[Long]
    )
    val filledForm = form.fill(1, "hello", Seq(1,2))
    assertEquals[Any, Any](filledForm("long").value, Option(1L))
    assertEquals[Any, Any](filledForm("text").value, Option("hello"))
    assertEquals[Any, Any](filledForm("seq").value, Option(Seq(1,2)))

    assertEquals[Any, Any](filledForm.data.get("long"), Option(1L))
    assertEquals[Any, Any](filledForm.data.get("text"), Option("hello"))
    assertEquals[Any, Any](filledForm.data.get("seq"), Option(Seq(1,2)))

    assertEquals(filledForm.value, Option((1L, "hello", Seq(1L, 2L))))
  }

  test("bind tuple 3"){
    val form = Form.tuple(
      "name" -> text,
      "birthday" -> localDate
    )
    val filledForm = form.fill("Homer", LocalDate.parse("1990-01-01"))
    assertEquals(filledForm[LocalDate]("birthday").value, Some(LocalDate.parse("1990-01-01")))
  }

  test("bind as JSON"){
    val form = Form.tuple(
      "name" -> text,
      "age" -> number,
      "isActive" -> boolean,
      "id" -> longNumber,
      "balance" -> float,
      "remaining" -> double,
      "birthday" -> localDate
    )
    val jsonObject = Json.obj(
      "name" -> "Homer", "age" -> 50, "isActive" -> true, "id" -> 123456L, "balance" -> 100.12F,
      "remaining" -> 100.00, "birthday" -> LocalDate.parse("1990-01-01")
    )

    val boundForm = form.bind(jsonObject)
    assertEquals[Any, Any](boundForm.data.get("name"), Some("Homer"))
    assertEquals[Any, Any](boundForm.data.get("age"), Some(50))
    assertEquals[Any, Any](boundForm.data.get("isActive"), Some(true))
    assertEquals[Any, Any](boundForm.data.get("id"), Some(123456L))
    assertEquals[Any, Any](boundForm.data.get("balance"), Some(100.12F))
    assertEquals[Any, Any](boundForm.data.get("remaining"), Some(100.00))
    assertEquals[Any, Any](boundForm.data.get("birthday"), Some(LocalDate.parse("1990-01-01")))

    val fields = boundForm.mappings.toList.asInstanceOf[List[Field[?]]]
    assertEquals[Any, Any](fields(0).value, Some("Homer"))
    assertEquals[Any, Any](fields(1).value, Some(50))
    assertEquals[Any, Any](fields(2).value, Some(true))
    assertEquals[Any, Any](fields(3).value, Some(123456L))
    assertEquals[Any, Any](fields(4).value, Some(100.12F))
    assertEquals[Any, Any](fields(5).value, Some(100.00))
    assertEquals[Any, Any](fields(6).value, Some(LocalDate.parse("1990-01-01")))
    assertEquals(boundForm.value, Some(("Homer", 50, true, 123456L, 100.12F, 100.00, LocalDate.parse("1990-01-01"))))
    assertEquals(boundForm.data.size, 7)

  }

  test("case class 2") {
    case class Foo(l: Long, s: String)
    val form: Form[Foo] = Form.mapping[Foo](
      "l" -> Field.of[Long],
      "s" -> Field.of[String]
    )
    val filledForm = form.fill(Foo(1, "hello"))
    assertEquals[Any, Any](filledForm.data.get("l"), Option(1))
    assertEquals[Any, Any](filledForm.data.get("s"), Option("hello"))

    assertEquals[Any, Any](filledForm("l").value, Option(1))
    assertEquals[Any, Any](filledForm("s").value, Option("hello"))
  }

  test("case class 3") {
    case class Foo(l: Long, s: String, xs: Seq[Long])
    val form: Form[Foo] = Form.mapping[Foo](
      "l" -> longNumber,
      "s" -> text,
      "xs" -> seq[Long]
    )
    val filledForm = form.fill(Foo(1, "hello", Seq(1,2)))
    assertEquals[Any, Any](filledForm.data.get("l"), Option(1))
    assertEquals[Any, Any](filledForm.data.get("s"), Option("hello"))
    assertEquals[Any, Any](filledForm.data.get("xs"), Option(Seq(1,2)))
    assertEquals[Any, Any](filledForm("l").value, Option(1))
    assertEquals[Any, Any](filledForm("s").value, Option("hello"))
    assertEquals[Any, Any](filledForm("xs").value, Option(Seq(1,2)))
  }


  test("valid form fill"){
    val form: Form[(Long, String, Seq[Long])] = Form.tuple(
      "l" -> longNumber,
      "s" -> text,
      "xs" -> seq[Long]
    )
    val filledForm = form.fill((1L, "text", Seq(1L, 2L)))
    assertEquals[Any, Any](filledForm.data.get("l"), Some(1L))
    assertEquals[Any, Any](filledForm.data.get("s"), Some("text"))
    assertEquals[Any, Any](filledForm.data.get("xs"), Some(Seq(1L, 2L)))
  }

  test("valid default, checked type"){
    val form: Form[(String, Boolean)] = Form.tuple(
      "s" ->  default(text, "Foo"),
      "xs" -> checked("this should be checked")
    )

    val filledForm = form.bind(      "s" -> null, "xs" -> "true")
    filledForm.fold(
      errorForm=> fail("should not have errors"),
      data=> {
        println(s"data = ${data}")
        assertEquals(data, ("Foo", true))
      }
    )
  }

}
