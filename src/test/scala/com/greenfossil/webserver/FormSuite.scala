package com.greenfossil.webserver

import com.greenfossil.webserver.data.*

class FormSuite extends munit.FunSuite {

  test("tuple 2") {
    val form: Form[(Long, String)] = Form.asTuple(
      "long" -> longNumber,
      "text" -> text
    )
    val filledForm = form.fill(1, "hello")
    assertEquals(filledForm.data.size, 2)
    assertEquals(filledForm.data("long"), 1)
    assertEquals(filledForm.data("text"), "hello")
    assertEquals[Any, Any](filledForm("long").value, Option(1))
    assertEquals[Any, Any](filledForm("text").value, Option("hello"))
  }

  test("tuple") {
    val form: Form[(Long, String, Seq[Long])] = Form.asTuple(
      "long" -> longNumber,
      "text" -> text,
      "seq" -> seq[Long]
    )
    val filledForm = form.fill(1, "hello", Seq(1,2))
    assertEquals(filledForm.data.size, 3)
    assertEquals(filledForm.data("long"), 1)
    assertEquals(filledForm.data("text"), "hello")
    assertEquals(filledForm.data("seq"), Seq(1,2))
  }

  test("case class 2") {
    case class Foo(l: Long, s: String)
    val form: Form[Foo] = Form.asClass[Foo](
      "long" -> longNumber,
      "text" -> text
    )
    val filledForm = form.fill(Foo(1, "hello"))
    assertEquals(filledForm.data.size, 2)
    assertEquals(filledForm.data("long"), 1)
    assertEquals(filledForm.data("text"), "hello")
  }
//
//  test("case class 3") {
//    case class Foo(l: Long, s: String, xs: Seq[Long])
//    val form: Form[Foo] = Form.asClass(
//      "long" -> longNumber,
//      "text" -> text,
//      "seq" -> seq[Long]
//    )
//    val filledForm = form.fill(Foo(1, "hello", Seq(1,2)))
//    assertEquals(filledForm.data.size, 3)
//    assertEquals(filledForm.data("long"), 1)
//    assertEquals(filledForm.data("text"), "hello")
//    assertEquals(filledForm.data("seq"), Seq(1,2))
//  }


//  test("CaseClass single field") {
//    case class Class(string: String)
//    val form = Form(
//
//    )
//  }

//  test("Case Class "){
//    case class Group(long: Long, string: String, opt: Option[String], seq:Seq[Long])
//    val groupForm = Form(
//      mapping(
//        "long" -> longNumber,
//        "string" -> text,
//        "opt" -> optional(text),
//        "seq" -> seq(longNumber))(Group.apply)(Group.unapply))
//  }

}
