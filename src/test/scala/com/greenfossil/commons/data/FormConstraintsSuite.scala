package com.greenfossil.commons.data

class FormConstraintsSuite extends munit.FunSuite {

  case class UserData(name: String, age: Int)

  test("case class Form - field-based verify") {
    val userFormConstraints = Form
      .mapping[UserData](
        "name" -> nonEmptyText,
        "age"  -> number.verifying("Age must be 42", age => age == 42)
      )

    val bindedForm = userFormConstraints.bind(Map("name" -> "1", "age" -> "10"))
    assertEquals(bindedForm.hasErrors, true)
    assertNoDiff(bindedForm.errors.head.messages.head, "Age must be 42")
  }

  test("case class Form - form-based verifying success") {
    val userFormConstraints = Form
      .mapping[UserData](
        "name" -> text,
        "age"  -> number
      ).verifying("Name should be homer and age is 42", user => user.name == "homer" && user.age == 42)
    val bindedForm = userFormConstraints.bind(Map("name" -> "homer", "age" -> "10"))
    assertEquals(bindedForm.hasErrors, true)
    assertNoDiff(bindedForm.errors.head.messages.head, "Name should be homer and age is 42")
  }

  test("case class Form - form-based verifying failure") {
    val userFormConstraints = Form
      .mapping[UserData](
        "name" -> text,
        "age"  -> number
      ).verifying("Name should be homer and age is 42", user => user.name == "homer" && user.age == 42)
    val boundForm = userFormConstraints.bind(Map("name" -> "homer", "age" -> "42"))
    assertEquals(boundForm.hasErrors, false)
  }

  test("form verifying") {
    val userFormConstraints:Form[UserData] = Form.mapping[UserData](
        "name" -> text,
        "age"  -> number
      ).verifying("Bad data", userData => true)

    val tuple:Form[(String, Int)] = Form.tuple(
      "name" -> text, //FIXME .transform[Int](s => s.toInt, int => int.toString),
      "age"  -> number
    ).verifying("Bad data", tuple => true)

  }

}
