package com.greenfossil.commons.data

class FormConstraintsSuite extends munit.FunSuite {

  case class UserData(name: String, age: Int)

  test("case class Form - field-based verify") {
    val userFormConstraints =
      mapping[UserData](
        "name" -> nonEmptyText,
        "age"  -> number.verifying("Age must be 42", age => age == 42)
      )

    val boundForm = userFormConstraints.bind("name" -> "1", "age" -> "10")
    assertEquals(boundForm.hasErrors, true)
    assertNoDiff(boundForm.errors.head.messages.head, "Age must be 42")
  }

  test("case class Form - form-based verifying success") {
    val userFormConstraints =
      mapping[UserData](
        "name" -> text,
        "age"  -> number
      ).verifying("Name should be homer and age is 42", user => user.name == "homer" && user.age == 42)
    val boundForm = userFormConstraints.bind("name" -> "homer", "age" -> "10")
    assertEquals(boundForm.hasErrors, true)
    assertNoDiff(boundForm.errors.head.messages.head, "Name should be homer and age is 42")
  }

  test("case class Form - form-based verifying failure") {
    val userFormConstraints =
      mapping[UserData](
        "name" -> text,
        "age"  -> number
      ).verifying("Name should be homer and age is 42", user => user.name == "homer" && user.age == 42)
    val boundForm = userFormConstraints.bind("name" -> "homer", "age" -> "42")
    assertEquals(boundForm.hasErrors, false)
  }

  test("form verifying") {
    val userFormConstraints:Mapping[UserData] = mapping[UserData](
        "name" -> text,
        "age"  -> number
      ).verifying("Bad data", userData => true)

    val tupleField:Mapping[(String, Int)] = tuple(
      "name" -> text, //FIXME .transform[Int](s => s.toInt, int => int.toString),
      "age"  -> number
    ).verifying("Bad data", tuple => true)

  }

  test("form optional binding with verifying"){
    val optionalField = optional[String].verifying("Test error", strOpt => strOpt.exists(s => s.equals("test")))
    val form = optionalField.name("foo")

    assertEquals[Any, Any](form.bind("foo" -> "test").value, Some("test"))
    assert(form.bind("foo" -> "abc").errors.exists(_.message == "Test error"))
  }

  test("form seq binding with verifying"){
    val optionalField = seq[String].verifying("Test error", xs => xs.exists(s => s.equals("test")))
    val form = optionalField.name("foo")

    assertEquals(form.bind("foo[1]" -> "test").value, Some(Seq("test")))
    assert(form.bind("foo[1]" -> "abc").errors.exists(_.message == "Test error"))
  }

}
