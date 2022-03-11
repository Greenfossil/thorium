package com.greenfossil.webserver

import com.greenfossil.webserver.data.Field
import com.greenfossil.webserver.data.*

class FieldConstraintsSuite extends munit.FunSuite {

  /*
   * For implementing constraints - use play.api.data.Forms for help. Need to use Constraints class
   */

  test("nonEmptyText") {
    val f1 = nonEmptyText
    val boundedF1 = f1.bind("")
    println(s"boundedF = ${boundedF1}")
    assertNoDiff(boundedF1.errors.head.messages.head, "error.required")

    val f2 = nonEmptyText
    val boundedF2 = f2.bind("hello")
    println(s"boundedF = ${boundedF2}")
    assert(boundedF2.errors.isEmpty)
  }

  import java.time.*
  test("LocalDate bind"){
    //broken Field.toValueOf
    val f = Field.of[LocalDate]("f")
    val boundField = f.bind(LocalDate.now().toString)
    assertEquals(boundField.value, Option(LocalDate.now))
  }

  test("LocalTime bind"){
    //Missing Field.fieldType definition
    val now = LocalTime.now
//    val f = Field.of[LocalTime]("f")
//    val boundField = f.bind(now.toString)
//    assertEquals(boundField.value, Option(now))
  }

}
