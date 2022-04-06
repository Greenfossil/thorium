package com.greenfossil.commons.data

class FormErrorSuite extends munit.FunSuite{

  test("Field errors"){
    val bindedForm = Form.tuple("name" -> nonEmptyText, "value" -> text).bind(Map("name" -> "", "value" -> ""))
    assertEquals(bindedForm.error("name").map(_.message),  Some("error.required"))
  }

  test("Form.withError"){
    val form = Form.tuple("name" -> nonEmptyText, "value" -> text).withError("name", "error.required")
    assertEquals(form.error("name").map(_.message),  Some("error.required"))
  }

  test("Form.globalErrors"){
    val form = Form.tuple("name" -> nonEmptyText, "value" -> text).withGlobalError("This is a sample error")
    assertEquals(form.globalErrors.headOption.map(_.message),  Some("This is a sample error"))
  }

  test("Form.discardingErrors"){
    val errorForm = Form.tuple("name" -> nonEmptyText, "value" -> text).bind(Map("name" -> "", "value" -> ""))
    assert(errorForm.hasErrors)
    assertEquals(errorForm.discardingErrors.hasErrors, false)
  }

}