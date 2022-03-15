package com.greenfossil.webserver

import com.greenfossil.commons.json.Json
import com.greenfossil.webserver.data.{*, given}

import java.time.LocalDate

class FormSuite extends munit.FunSuite {

  test("tuple 2") {
    val form: Form[(Long, String)] = Form.tuple(
      "long" -> longNumber,
      "text" -> text
    )
    val filledForm = form.fill(1, "hello")
    filledForm.data
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

  test("bind tuple 2") {
    val form: Form[(Long, String, Seq[Int])] = Form.tuple(
      "long" -> longNumber,
      "text" -> text,
      "seq" -> seq[Int]
    )
    val bindedForm = form.bind(Map("long" -> Seq("1"), "text" -> Seq("hello"), "seq" -> Seq("1", "2")))
    assertEquals(bindedForm.data.size, 3)
    val a = bindedForm("long")
    val x = bindedForm("long").value
    assertEquals[Any, Any](bindedForm("long").value, Option(1))
    assertEquals[Any, Any](bindedForm("text").value, Option("hello"))
    assertEquals[Any, Any](bindedForm("seq").value, Option(Seq(1,2)))

    assertEquals[Any, Any](bindedForm.data.get("long"), Option(1))
    assertEquals[Any, Any](bindedForm.data.get("text"), Option("hello"))
    assertEquals[Any, Any](bindedForm.data.get("seq"), Option(Seq(1,2)))


    assertEquals(bindedForm.value, Option((1L, "hello", Seq(1, 2))))
  }

  test("bind tuple 3"){
    val form = Form.tuple(
      "name" -> text,
      "birthday" -> localDate
    )
    val filledForm = form.fill("Homer", LocalDate.parse("1990-01-01"))
    assertEquals(filledForm[LocalDate]("birthday").value, Some(LocalDate.parse("1990-01-01")))

    val bindedForm = form.bind(Map("name" -> Seq("Homer"), "birthday" -> Seq("1990-01-01")))
    assertEquals[Any, Any](bindedForm("birthday").value, Some(LocalDate.parse("1990-01-01")))
    assertEquals[Any, Any](bindedForm.data.get("birthday"), Some(LocalDate.parse("1990-01-01")))
    assertEquals(bindedForm.value, Some(("Homer", LocalDate.parse("1990-01-01"))))
    assertEquals(bindedForm.data.size, 2)
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

    val bindedForm = form.bind(jsonObject, Map.empty)
    assertEquals[Any, Any](bindedForm.data.get("name"), Some("Homer"))
    assertEquals[Any, Any](bindedForm.data.get("age"), Some(50))
    assertEquals[Any, Any](bindedForm.data.get("isActive"), Some(true))
    assertEquals[Any, Any](bindedForm.data.get("id"), Some(123456L))
    assertEquals[Any, Any](bindedForm.data.get("balance"), Some(100.12F))
    assertEquals[Any, Any](bindedForm.data.get("remaining"), Some(100.00))
    assertEquals[Any, Any](bindedForm.data.get("birthday"), Some(LocalDate.parse("1990-01-01")))

    val fields = bindedForm.mappings.toList.asInstanceOf[List[Field[_]]]
    assertEquals[Any, Any](fields(0).value, Some("Homer"))
    assertEquals[Any, Any](fields(1).value, Some(50))
    assertEquals[Any, Any](fields(2).value, Some(true))
    assertEquals[Any, Any](fields(3).value, Some(123456L))
    assertEquals[Any, Any](fields(4).value, Some(100.12F))
    assertEquals[Any, Any](fields(5).value, Some(100.00))
    assertEquals[Any, Any](fields(6).value, Some(LocalDate.parse("1990-01-01")))
    assertEquals(bindedForm.value, Some(("Homer", 50, true, 123456L, 100.12F, 100.00, LocalDate.parse("1990-01-01"))))
    assertEquals(bindedForm.data.size, 7)

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

    val bindedMappingForm1 = form.bind(Map("l" -> Seq("1"), "s" -> Seq("hello"), "xs[]" -> Seq("1", "2")))
    assertEquals[Any, Any](bindedMappingForm1.data.get("l"), Option(1))
    assertEquals[Any, Any](bindedMappingForm1.data.get("s"), Option("hello"))
    assertEquals[Any, Any](bindedMappingForm1.data.get("xs"), Option(Seq(1, 2)))
    assertEquals[Any, Any](bindedMappingForm1("l").value, Option(1))
    assertEquals[Any, Any](bindedMappingForm1("s").value, Option("hello"))
    assertEquals[Any, Any](bindedMappingForm1("xs").value, Option(Seq(1, 2)))

    val bindedMappingForm2 = form.bind(Map("l" -> Seq("1"), "s" -> Seq("hello"), "xs[0]" -> Seq("1"), "xs[1]" -> Seq("2")))
    assertEquals[Any, Any](bindedMappingForm2.data.get("l"), Option(1))
    assertEquals[Any, Any](bindedMappingForm2.data.get("s"), Option("hello"))
    assertEquals[Any, Any](bindedMappingForm2.data.get("xs"), Option(Seq(1, 2)))
    assertEquals[Any, Any](bindedMappingForm2("l").value, Option(1))
    assertEquals[Any, Any](bindedMappingForm2("s").value, Option("hello"))
    assertEquals[Any, Any](bindedMappingForm2("xs").value, Option(Seq(1, 2)))

    val jsonBindedForm = form.bind(Json.obj("l" -> 1, "s" -> "hello", "xs" -> Json.arr(1, 2)), Map.empty)
    assertEquals[Any, Any](jsonBindedForm.data.get("l"), Some(1))
    assertEquals[Any, Any](jsonBindedForm.data.get("s"), Some("hello"))
    assertEquals[Any, Any](jsonBindedForm.data.get("xs"), Some(Seq(1, 2)))
    val jsonFields = jsonBindedForm.mappings.toList.asInstanceOf[List[Field[_]]]
    assertEquals[Any, Any](jsonFields(0).value, Some(1))
    assertEquals[Any, Any](jsonFields(1).value, Some("hello"))
    assertEquals[Any, Any](jsonFields(2).value, Some(Seq(1, 2)))
  }


  test("valid bind and fold"){

    val form: Form[(Long, String, Seq[Long])] = Form.tuple(
      "l" -> longNumber,
      "s" -> text,
      "xs" -> seq[Long]
    )

    val data = Map(
      "l" -> 1L,
      "s" -> "text",
      "xs" -> Seq(1L, 2L),
    )

    form.bind(data).fold(
      errorForm => fail("Should not have error form"),
      data => assertEquals(data, (1L, "text", Seq(1L, 2L)))
    )
  }

  test("invalid bind and fold"){

    val form: Form[(Long, String, Seq[Long])] = Form.tuple(
      "l" -> longNumber(1,2, true),
      "s" -> text,
      "xs" -> seq[Long]
    )

    val data = Map(
      "l" -> 10L,
      "s" -> "text",
      "xs" -> Seq(1L, 2L),
    )

    form.bind(data).fold(
      errorForm => {
        assertEquals(errorForm.errors.size, 1)
      },
      data => fail("should not return invalid data")
    )
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


  test("invalid ignored, default, checked type"){
    val form: Form[(/*Long, */String, Boolean)] = Form.tuple(
//      "l" -> ignored[Long](0L),
      "s" ->  default(text, "Foo"),
      "xs" -> checked("this should be checked")
    )
    val data = Map(
      "l" -> 10L,
      "xs" -> false,
    )

    val filledForm = form.bind(data)
    filledForm.fold(
      errorForm=> assertEquals(errorForm.errors.size, 1),
      data=> fail("Should not give invalid data")
    )
  }

  test("valid ignored, default, checked type"){
    val form: Form[(String, Boolean)] = Form.tuple(
//      "l" -> ignored[Long](0L),
      "s" ->  default(text, "Foo"),
      "xs" -> checked("this should be checked")
    )
    val data = Map(
//      "l" -> 1L,
      "s" -> null,
      "xs" -> true,
    )

    val filledForm = form.bind(data)
    filledForm.fold(
      errorForm=> fail("should not have errors"),
      data=> {
        println(s"data = ${data}")
        assertEquals(data, ("Foo", true))
      }
    )
  }

}
