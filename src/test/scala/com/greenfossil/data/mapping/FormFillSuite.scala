package com.greenfossil.data.mapping

import com.greenfossil.commons.json.Json

import java.time.LocalDate

class FormFillSuite extends munit.FunSuite {

  import Mapping.*

  test("tuple 2") {
    val form = tuple(
      "long" -> longNumber,
      "text" -> text
    )
    val filledForm = form.fill(1, "hello")
    assertEquals[Any, Any](filledForm("long").value, Option(1))
    assertEquals[Any, Any](filledForm("text").value, Option("hello"))

    assertEquals(filledForm.value, Some((1L, "hello")))
  }

  test("fill tuple") {
    val form = tuple(
      "long" -> longNumber,
      "text" -> text,
      "seq" -> seq[Long]
    )
    val filledForm = form.fill(1, "hello", Seq(1,2))
    assertEquals[Any, Any](filledForm("long").value, Option(1L))
    assertEquals[Any, Any](filledForm("text").value, Option("hello"))
    assertEquals[Any, Any](filledForm("seq").value, Option(Seq(1,2)))

    assertEquals(filledForm.value, Option((1L, "hello", Seq(1L, 2L))))
  }

  test("bind tuple 3"){
    val form = tuple(
      "name" -> text,
      "birthday" -> localDate
    )
    val filledForm = form.fill("Homer", LocalDate.parse("1990-01-01"))
    assertEquals[Any, Any](filledForm("birthday").value, Some(LocalDate.parse("1990-01-01")))
  }

  test("bind as JSON"){
    val form = tuple(
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
    assertEquals[Any, Any](boundForm("name").value, Some("Homer"))
    assertEquals[Any, Any](boundForm("age").value, Some(50))
    assertEquals[Any, Any](boundForm("isActive").value, Some(true))
    assertEquals[Any, Any](boundForm("id").value, Some(123456L))
    assertEquals[Any, Any](boundForm("balance").value, Some(100.12F))
    assertEquals[Any, Any](boundForm("remaining").value, Some(100.00))
    assertEquals[Any, Any](boundForm("birthday").value, Some(LocalDate.parse("1990-01-01")))

    assertEquals(boundForm.value, Some(("Homer", 50, true, 123456L, 100.12F, 100.00, LocalDate.parse("1990-01-01"))))
//    assertEquals(boundForm.size, 7)

  }

  test("case class 2") {
    case class Foo(l: Long, s: String)
    val form = mapping[Foo](
      "l" -> Mapping.mapTo[Long],
      "s" -> Mapping.mapTo[String]
    )
    val filledForm = form.fill(Foo(1, "hello"))
    assertEquals[Any, Any](filledForm("l").value, Option(1))
    assertEquals[Any, Any](filledForm("s").value, Option("hello"))

  }

  test("case class 3") {
    case class Foo(l: Long, s: String, xs: Seq[Long])
    val form: Mapping[Foo] = mapping[Foo](
      "l" -> longNumber,
      "s" -> text,
      "xs" -> seq[Long]
    )
    val filledForm = form.fill(Foo(1, "hello", Seq(1,2)))
    assertEquals[Any, Any](filledForm("l").value, Option(1))
    assertEquals[Any, Any](filledForm("s").value, Option("hello"))
    assertEquals[Any, Any](filledForm("xs").value, Option(Seq(1,2)))

  }


  test("valid form fill"){
    val form: Mapping[(Long, String, Seq[Long])] = tuple(
      "l" -> longNumber,
      "s" -> text,
      "xs" -> seq[Long]
    )
    val filledForm = form.fill((1L, "text", Seq(1L, 2L)))
    assertEquals[Any, Any](filledForm("l").value, Some(1L))
    assertEquals[Any, Any](filledForm("s").value, Some("text"))
    assertEquals[Any, Any](filledForm("xs").value, Some(Seq(1L, 2L)))
  }

  test("valid default, checked type"){
    val form: Mapping[(String, Boolean)] = tuple(
      "defaultText" ->  default(text, "Foo"),
      "isChecked" -> checked("this should be checked")
    )

    val filledForm = form.bind(      "defaultText" -> null, "isChecked" -> "true")
    filledForm.fold(
      errorForm=> fail("should not have errors"),
      data=> {
        assertEquals(data, ("Foo", true))
      }
    )
  }

}