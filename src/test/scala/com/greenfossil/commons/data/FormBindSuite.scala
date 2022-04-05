package com.greenfossil.commons.data

import com.greenfossil.commons.json.Json

import java.time.LocalDate

class FormBindSuite extends munit.FunSuite {

  test("bind tuple 2") {
    val form: Form[(Long, String, Seq[Int])] = Form.tuple(
      "long" -> longNumber,
      "text" -> text,
      "seq" -> seq[Int]
    )
    val bindedForm = form.bind(Map("long" -> "1", "text" -> "hello", "seq[1]" -> "1", "seq[2]" -> "2"))
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

    val bindedForm = form.bind(Map("name" -> "Homer", "birthday" -> "1990-01-01"))
    assertEquals[Any, Any](bindedForm("birthday").value, Some(LocalDate.parse("1990-01-01")))
    assertEquals[Any, Any](bindedForm.data.get("birthday"), Some(LocalDate.parse("1990-01-01")))
    assertEquals(bindedForm.value, Some(("Homer", LocalDate.parse("1990-01-01"))))
    assertEquals(bindedForm.data.size, 2)
  }

  test("bind tuple 4 - longnumber"){
    val form = Form.tuple(
      "l1" -> longNumber,
      "l2" -> optional[Long]
    )
    val bindedForm = form.bind(Map("l1" -> "1", "l2" -> ""))
    assertEquals[Any, Any](bindedForm.value, Some((1L, None)))

    val bindedForm2 = form.bind(Map("l1" -> "1")) //test absence of l2
    assertEquals[Any,Any](bindedForm2.value, Some((1L, None)))
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

    val bindedForm = form.bind(jsonObject)
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

  test("case class 3") {
    case class Foo(l: Long, s: String, xs: Seq[Long])
    val form: Form[Foo] = Form.mapping[Foo](
      "l" -> longNumber,
      "s" -> text,
      "xs" -> seq[Long]
    )

    val bindedMappingForm1 = form.bind(Map("l" -> "1", "s" -> "hello", "xs[1]" -> "1", "xs[2]" -> "2"))
    assertEquals[Any, Any](bindedMappingForm1.data.get("l"), Option(1))
    assertEquals[Any, Any](bindedMappingForm1.data.get("s"), Option("hello"))
    assertEquals[Any, Any](bindedMappingForm1.data.get("xs"), Option(Seq(1, 2)))
    assertEquals[Any, Any](bindedMappingForm1("l").value, Option(1))
    assertEquals[Any, Any](bindedMappingForm1("s").value, Option("hello"))
    assertEquals[Any, Any](bindedMappingForm1("xs").value, Option(Seq(1, 2)))

    val bindedMappingForm2 = form.bind(Map("l" -> "1", "s" -> "hello", "xs[0]" -> "1", "xs[1]" -> "2"))
    assertEquals[Any, Any](bindedMappingForm2.data.get("l"), Option(1))
    assertEquals[Any, Any](bindedMappingForm2.data.get("s"), Option("hello"))
    assertEquals[Any, Any](bindedMappingForm2.data.get("xs"), Option(Seq(1, 2)))
    assertEquals[Any, Any](bindedMappingForm2("l").value, Option(1))
    assertEquals[Any, Any](bindedMappingForm2("s").value, Option("hello"))
    assertEquals[Any, Any](bindedMappingForm2("xs").value, Option(Seq(1, 2)))

    val jsonBindedForm = form.bind(Json.obj("l" -> 1, "s" -> "hello", "xs" -> Json.arr(1, 2)))
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
      "l" -> "1",
      "s" -> "text",
      "xs[1]" -> "1",
      "xs[2]" -> "2"
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
      "l" -> "10",
      "s" -> "text",
      "xs[1]" -> "1",
      "xs[2]" -> "2"
    )

    form.bind(data).fold(
      errorForm => {
        assertEquals(errorForm.errors.size, 1)
      },
      data => fail("should not return invalid data")
    )
  }

  //Discuss on Checked api expection?
  test("invalid default, checked type"){
    val form: Form[(String, Boolean)] = Form.tuple(
      "s" ->  default(text, "Foo"),
      "xs" -> checked("this should be checked")
    )
    val data = Map("xs" -> "true")

    val bindedForm = form.bind(data)
    bindedForm.fold(
      errorForm=> assertEquals(errorForm.errors.size, 1),
      data=> fail("Should not give invalid data")
    )
  }

  test("valid default, checked type"){
    val form: Form[(String, Boolean)] = Form.tuple(
      "s" ->  default(text, "Foo"),
      "xs" -> checked("this should be checked")
    )
    val data = Map(
      "s" -> null,
      "xs" -> "true",
    )

    val bindedForm = form.bind(data)
    bindedForm.fold(
      errorForm=> fail("should not have errors"),
      data=> {
        println(s"data = ${data}")
        assertEquals(data, ("Foo", true))
      }
    )
  }

  test("bind tuple 1"){
    val form = Form("name", text).bind(Map("name" -> "Homer"))
    assertEquals(form.value, Some("Homer"))

    form.fold(
      errorForm => fail("Should not have error"),
      name => {
        assertNoDiff(name, "Homer")
      }
    )
  }

  test("bind optional[String]"){
    val form = Form.tuple("name" -> optional[String], "age" -> optional[Int])
    val bindedForm = form.bind(Map("name" -> "Homer", "age" -> "50"))
    assertEquals(bindedForm.value, Some(("Homer", 50)))

    bindedForm.fold(
      errorForm => fail("should not have error"),
      {
        case (nameOpt, ageOpt) =>
          assertEquals(nameOpt, "Homer")
          assertEquals(ageOpt, 50)
      }
    )
  }

  test("bind optional(text)"){
    val form = Form.tuple("name" -> optional(text), "age" -> optional(number))
    val bindedForm = form.bind(Map("name" -> "Homer", "age" -> "50"))
    assertEquals(bindedForm.value, Some(("Homer", 50)))

    bindedForm.fold(
      errorForm => fail("should not have error"),
      {
        case (nameOpt, ageOpt) =>
          assertEquals(nameOpt, "Homer")
          assertEquals(ageOpt, 50)
      }
    )
  }

}
