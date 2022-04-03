package com.greenfossil.commons.data

class FormConstraintsSuite extends munit.FunSuite {

  case class UserData(name: String, age: Int)

  test("case class Form - field-based verify") {
    val userFormConstraints = Form
      .mapping[UserData](
        "name" -> nonEmptyText,
        "age"  -> number.verifying("Age must be 42", age => age == 42)
      )

    val boundForm = userFormConstraints.bind(Map("bob" -> Seq("1"), "age" -> Seq("10")))
    assertEquals(boundForm.hasErrors, true)
    assertNoDiff(boundForm.errors.head.messages.head, "Age must be 42")
  }

  test("case class Form - form-based verifying success") {
    val userFormConstraints = Form
      .mapping[UserData](
        "name" -> text,
        "age"  -> number
      ).verifying("Name should be homer and age is 42", user => user.name == "homer" && user.age == 42)
    val boundForm = userFormConstraints.bind(Map("name" -> Seq("homer"), "age" -> Seq("10")))
    assertEquals(boundForm.hasErrors, true)
    assertNoDiff(boundForm.errors.head.messages.head, "Name should be homer and age is 42")
  }

  test("case class Form - form-based verifying failure") {
    val userFormConstraints = Form
      .mapping[UserData](
        "name" -> text,
        "age"  -> number
      ).verifying("Name should be homer and age is 42", user => user.name == "homer" && user.age == 42)
    val boundForm = userFormConstraints.bind(Map("name" -> Seq("homer"), "age" -> Seq("42")))
    assertEquals(boundForm.hasErrors, false)
  }

  test("form verifying") {
    val userFormConstraints:Form[UserData] = Form.mapping[UserData](
        "name" -> text,
        "age"  -> number
      ).verifying("Bad data", userData => true)

    val tuple = Form.tuple(
      "name" -> text.transform[Int](s => s.toInt, int => int.toString),
      "age"  -> number
    ).verifying("Bad data", (x: Int, y: Int) => true)

  }

}
