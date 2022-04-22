package com.greenfossil.commons.data

import com.greenfossil.commons.json.Json

import java.time.LocalDate

class FormBindSuite extends munit.FunSuite {
  import Mapping.*
  
  test("bind tuple 2") {
    val form: Mapping[(Long, String, Seq[Int])] = tuple(
      "long" -> longNumber,
      "text" -> text,
      "seq" -> seq[Int]
    )
    val boundForm = form.bind("long" -> "1", "text" -> "hello", "seq[1]" -> "1", "seq[2]" -> "2")
//    assertEquals(boundForm.data.size, 3)
    val a = boundForm("long")
    val x = boundForm("long").value
    assertEquals[Any, Any](boundForm("long").value, Option(1))
    assertEquals[Any, Any](boundForm("text").value, Option("hello"))
    assertEquals[Any, Any](boundForm("seq").value, Option(Seq(1,2)))

//    assertEquals[Any, Any](boundForm.data.get("long"), Option(1))
//    assertEquals[Any, Any](boundForm.data.get("text"), Option("hello"))
//    assertEquals[Any, Any](boundForm.data.get("seq"), Option(Seq(1,2)))

    assertEquals(boundForm.value, Option((1L, "hello", Seq(1, 2))))
  }

  test("bind tuple 3"){
    val form = tuple(
      "name" -> text,
      "birthday" -> localDate
    )

    val boundForm = form.bind("name" -> "Homer", "birthday" -> "1990-01-01")
    assertEquals[Any, Any](boundForm("birthday").value, Some(LocalDate.parse("1990-01-01")))
    assertEquals(boundForm.value, Some(("Homer", LocalDate.parse("1990-01-01"))))
//    assertEquals(boundForm.data.size, 2)
  }

  test("bind tuple 4 - longnumber"){
    val form = tuple(
      "l1" -> longNumber,
      "l2" -> optional[Long]
    )
    val boundForm = form.bind("l1" -> "1", "l2" -> "")
    assertEquals(boundForm.value, Some((1L, None)))

    val boundForm2 = form.bind("l1" -> "1") //test absence of l2
    assertEquals(boundForm2.value, Some((1L, None)))
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
//    assertEquals(boundForm.data.size, 7)

  }

  test("case class 3") {
    case class Foo(l: Long, s: String, xs: Seq[Long])
    val form: Mapping[Foo] = mapping[Foo](
      "l" -> longNumber,
      "s" -> text,
      "xs" -> seq[Long]
    )

    val boundMappingForm1 = form.bind("l" -> "1", "s" -> "hello", "xs[1]" -> "1", "xs[2]" -> "2")
    assertEquals[Any, Any](boundMappingForm1("l").value, Option(1))
    assertEquals[Any, Any](boundMappingForm1("s").value, Option("hello"))
    assertEquals[Any, Any](boundMappingForm1("xs").value, Option(Seq(1, 2)))

    val boundMappingForm2 = form.bind("l" -> "1", "s" -> "hello", "xs[0]" -> "1", "xs[1]" -> "2")
    assertEquals[Any, Any](boundMappingForm2("l").value, Option(1))
    assertEquals[Any, Any](boundMappingForm2("s").value, Option("hello"))
    assertEquals[Any, Any](boundMappingForm2("xs").value, Option(Seq(1, 2)))

    val jsonboundForm = form.bind(Json.obj("l" -> 1, "s" -> "hello", "xs" -> Json.arr(1, 2)))
    assertEquals[Any, Any](jsonboundForm("l").value, Some(1))
    assertEquals[Any, Any](jsonboundForm("s").value, Some("hello"))
    assertEquals[Any, Any](jsonboundForm("xs").value, Some(Seq(1, 2)))
  }


  test("valid bind and fold"){

    val form: Mapping[(Long, String, Seq[Long])] = tuple(
      "l" -> longNumber,
      "s" -> text,
      "xs" -> seq[Long]
    )

    form.bind("l" -> "1", "s" -> "text", "xs[1]" -> "1", "xs[2]" -> "2")
      .fold(
        errorForm => fail("Should not have error form"),
        data => assertEquals(data, (1L, "text", Seq(1L, 2L)))
      )
  }

  test("invalid bind and fold"){

    val form: Mapping[(Long, String, Seq[Long])] = tuple(
      "l" -> longNumber(1,2, true),
      "s" -> text,
      "xs" -> seq[Long]
    )

    form.bind(   "l" -> "10", "s" -> "text", "xs[1]" -> "1", "xs[2]" -> "2").fold(
      errorForm => {
        assertEquals(errorForm.errors.size, 1)
      },
      data => fail("should not return invalid data")
    )
  }

  test("invalid default, checked type"){
    val form: Mapping[(String, Boolean)] = tuple(
      "defaultText" ->  default(text, "Foo"),
      "isChecked" -> checked("this should be checked")
    )

    val boundForm = form.bind("defaultText" -> "Bar", "isChecked" -> "true")
    boundForm.fold(
      errorForm=> fail("Should not have error"),
      data=>
        assertEquals(data, ("Bar", true))
    )
  }

  test("valid default, checked type"){
    val form: Mapping[(String, Boolean)] = tuple(
      "s" ->  default(text, "Foo"),
      "xs" -> checked("this should be checked")
    )

    val boundForm = form.bind(  "s" -> null, "xs" -> "true")
    boundForm.fold(
      errorForm=> fail("should not have errors"),
      data=> {
        assertEquals(data, ("Foo", true))
      }
    )
  }

  test("bind tuple 1"){
    val form = text.name("name").bind("name" -> "Homer")
    assertEquals(form.value, Some("Homer"))

    form.fold(
      errorForm => fail("Should not have error"),
      name => {
        assertNoDiff(name, "Homer")
      }
    )
  }

  test("bind tuple 1 optional[String]"){
    val form = optional[String].name("name")
    assertEquals[Any, Any](form.bind("name" -> "Homer").value, Some("Homer"))
  }

  test("bind optional[String]"){
    val form = tuple("name" -> optional[String], "age" -> optional[Int])
    val boundForm = form.bind("name" -> "Homer", "age" -> "50")
    assertEquals(boundForm.value, Some((Option("Homer"), Option(50))))

    boundForm.fold(
      errorForm => fail("should not have error"),
      {
        case (nameOpt, ageOpt) =>
          assertEquals(nameOpt, Option("Homer"))
          assertEquals(ageOpt, Option(50))
      }
    )
  }

  test("bind optional(text)"){
    val form = tuple("name" -> optional(text), "age" -> optional(number))
    val boundForm = form.bind("name" -> "Homer", "age" -> "50")
    assertEquals(boundForm.value, Some((Option("Homer"), Option(50))))

    boundForm.fold(
      errorForm => fail("should not have error"),
      {
        case (nameOpt, ageOpt) =>
          assertEquals(nameOpt, Option("Homer"))
          assertEquals(ageOpt, Option(50))
      }
    )
  }
  
  test("bind optional with no value"){
    val form = tuple("name" -> optional(text), "age" -> optional(number))
    val boundForm = form.bind("name" -> "Homer")
    assertEquals(boundForm.value, Some((Option("Homer"), None)))

    boundForm.fold(
      errorForm => {
        println(s"errorForm.errors = ${errorForm.errors}")
        fail("should not have error")
      },
      {
        case (nameOpt, ageOpt) =>
          assertEquals(nameOpt, Option("Homer"))
          assertEquals(ageOpt, None)
      }
    )
  }

}
