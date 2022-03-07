package com.greenfossil.webserver

import com.greenfossil.webserver.data.{*, given}

import java.time.LocalDate

class FormSuite extends munit.FunSuite {

  test("tuple 2") {
    val form: Form[(Long, String)] = Form.asTuple(
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
    val form: Form[(Long, String, Seq[Long])] = Form.asTuple(
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
    val form: Form[(Long, String, Seq[Int])] = Form.asTuple(
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
    val form = Form.asTuple(
      "name" -> text,
      "birthday" -> localDate
    )
    val filledForm = form.fill("Homer", LocalDate.parse("1990-01-01"))
    assertEquals(filledForm[LocalDate]("birthday").value, Some(LocalDate.parse("1990-01-01")))

    val bindedForm = form.bind(Map("name" -> Seq("Homer"), "birthday" -> Seq("1990-01-01")))
    assertEquals(bindedForm[LocalDate]("birthday").value, Some(LocalDate.parse("1990-01-01")))
  }

  test("bind as JSON"){

  }

  test("case class 2") {
    case class Foo(l: Long, s: String)
    val form = Form.asCaseClass[Foo](
      "l" -> Field.of[Long],
      "s" -> Field.of[String]
    )
    val filledForm = form.fill(Foo(1, "hello"))
    assertEquals(filledForm.data.size, 2)
    assertEquals(filledForm.data("long"), 1)
    assertEquals(filledForm.data("text"), "hello")
  }

  test("case class 3") {
    case class Foo(l: Long, s: String, xs: Seq[Long])
    val form = Form.asCaseClass[Foo](
      "l" -> longNumber,
      "s" -> text,
      "xs" -> seq[Long]
    )
    val filledForm = form.fill(Foo(1, "hello", Seq(1,2)))
    assertEquals(filledForm.data.size, 3)
    assertEquals(filledForm.data("long"), 1)
    assertEquals(filledForm.data("text"), "hello")
    assertEquals(filledForm.data("seq"), Seq(1,2))
  }

}
