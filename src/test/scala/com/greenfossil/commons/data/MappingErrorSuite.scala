package com.greenfossil.commons.data

class MappingErrorSuite extends munit.FunSuite{

  test("Field errors"){
    val boundForm = tuple("name" -> nonEmptyText, "value" -> text).bind("name" -> "", "value" -> "")
    assertEquals(boundForm.error("name").map(_.message),  Some("error.required"))
  }

  test("Form.withError"){
    val form = tuple("name" -> nonEmptyText, "value" -> text).withError("name", "error.required")
    assertEquals(form.error("name").map(_.message),  Some("error.required"))
  }

  test("Form.globalErrors"){
    val form = tuple("name" -> nonEmptyText, "value" -> text).withGlobalError("This is a sample error")
    assertEquals(form.globalErrors.headOption.map(_.message),  Some("This is a sample error"))
  }

  test("Form.discardingErrors"){
    val errorForm = tuple("name" -> nonEmptyText, "value" -> text).bind("name" -> "", "value" -> "")
    assert(errorForm.hasErrors)
    assertEquals(errorForm.discardingErrors.hasErrors, false)
  }

}
