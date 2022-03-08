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
//    assertEquals(filledForm.data.size, 2)
//    assertEquals(filledForm.data("long"), 1)
//    assertEquals(filledForm.data("text"), "hello")
    assertEquals[Any, Any](filledForm("long").value, Option(1))
    assertEquals[Any, Any](filledForm("text").value, Option("hello"))
  }

  test("fill tuple") {
    val form: Form[(Long, String, Seq[Long])] = Form.tuple(
      "long" -> longNumber,
      "text" -> text,
      "seq" -> seq[Long]
    )
    val filledForm = form.fill(1, "hello", Seq(1,2))
//    assertEquals(filledForm.data.size, 3)
    assertEquals[Any, Any](filledForm("long").value, Option(1))
    assertEquals[Any, Any](filledForm("text").value, Option("hello"))
    assertEquals[Any, Any](filledForm("seq").value, Option(Seq(1,2)))
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
  }

  test("bind tuple 3"){
    val form = Form.tuple(
      "name" -> text,
      "birthday" -> localDate
    )
    val filledForm = form.fill("Homer", LocalDate.parse("1990-01-01"))
    assertEquals(filledForm[LocalDate]("birthday").value, Some(LocalDate.parse("1990-01-01")))

    val bindedForm = form.bind(Map("name" -> Seq("Homer"), "birthday" -> Seq("1990-01-01")))
    assertEquals(bindedForm[LocalDate]("birthday").value, Some(LocalDate.parse("1990-01-01")))
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
    val fields = bindedForm.mappings.toList.asInstanceOf[List[Field[_]]]
    fields foreach println
    assertEquals[Any, Any](fields(0).value, Some("Homer"))
    assertEquals[Any, Any](fields(1).value, Some(50))
    assertEquals[Any, Any](fields(2).value, Some(true))
    assertEquals[Any, Any](fields(3).value, Some(123456L))
    assertEquals[Any, Any](fields(4).value, Some(100.12F))
    assertEquals[Any, Any](fields(5).value, Some(100.00))
    assertEquals[Any, Any](fields(6).value, Some(LocalDate.parse("1990-01-01")))
//    assertEquals(bindedForm.value, Some(("Homer", 50)))

  }

  test("case class 2") {
    case class Foo(l: Long, s: String)
    val form: Form[Foo] = Form.mapping[Foo](
      "l" -> Field.of[Long],
      "s" -> Field.of[String]
    )
    val filledForm = form.fill(Foo(1, "hello"))
//    assertEquals(filledForm.data.size, 2)
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
//    assertEquals(filledForm.data.size, 3)
    assertEquals[Any, Any](filledForm("l").value, Option(1))
    assertEquals[Any, Any](filledForm("s").value, Option("hello"))
    assertEquals[Any, Any](filledForm("xs").value, Option(Seq(1,2)))

    val bindedMappingForm1 = form.bind(Map("l" -> Seq("1"), "s" -> Seq("hello"), "xs[]" -> Seq("1", "2")))
    assertEquals[Any, Any](bindedMappingForm1("l").value, Option(1))
    assertEquals[Any, Any](bindedMappingForm1("s").value, Option("hello"))
    assertEquals[Any, Any](bindedMappingForm1("xs").value, Option(Seq(1, 2)))

    val bindedMappingForm2 = form.bind(Map("l" -> Seq("1"), "s" -> Seq("hello"), "xs[0]" -> Seq("1"), "xs[1]" -> Seq("2")))
    assertEquals[Any, Any](bindedMappingForm2("l").value, Option(1))
    assertEquals[Any, Any](bindedMappingForm2("s").value, Option("hello"))
    assertEquals[Any, Any](bindedMappingForm2("xs").value, Option(Seq(1, 2)))

    val jsonBindedForm = form.bind(Json.obj("l" -> 1, "s" -> "hello", "xs" -> Json.arr(1, 2)), Map.empty)
    val jsonFields = jsonBindedForm.mappings.toList.asInstanceOf[List[Field[_]]]
    jsonFields foreach println
    assertEquals[Any, Any](jsonFields(0).value, Some(1))
    assertEquals[Any, Any](jsonFields(1).value, Some("hello"))
    assertEquals[Any, Any](jsonFields(2).value, Some(Seq(1, 2)))
  }

}
