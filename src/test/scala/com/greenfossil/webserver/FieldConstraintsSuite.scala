package com.greenfossil.webserver

import com.greenfossil.webserver.data.*

class FieldConstraintsSuite extends munit.FunSuite {
  
  test("nonEmptyText") {
    val f = nonEmptyText
    val errorField = f.bind("")
    assertEquals(errorField.errors.size, 1)
    assertEquals(errorField.errors.head.message, "error.required")
    errorField.errors foreach println

    val validField = f.bind("abc")
    assert(validField.errors.isEmpty)
  }

  test("email"){

  }

  test("number"){

  }

  test("text"){

  }

  test("byte number"){

  }

  test("short number"){

  }

  test("long number"){

  }

  test("big decimal"){

  }

  test("custom constraint"){

  }
}
