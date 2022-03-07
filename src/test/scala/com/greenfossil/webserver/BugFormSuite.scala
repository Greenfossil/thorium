package com.greenfossil.webserver

import com.greenfossil.webserver.data.*

class BugFormSuite extends munit.FunSuite {

  test("Bind form"){
    val sampleForm = Form.asTuple(
      "name" -> text,
      "age" -> longNumber,
      "gender" -> text,
      "countries" -> seq[String],
      "maritalStatus" -> text,
      "tncAgree" -> optional[Boolean],
      "children" -> seq[String]
    )

    val filledForm = sampleForm.fill(
      "Homer Simpson", 21, "Male", Seq("Singapore"), "Married", Some(true),
      Seq("Marge Simpson", "Bart Simpson", "Lisa Simpson")
    )
    assertEquals[Any, Any](filledForm("name").value, Option("Homer Simpson"))
    filledForm.mappings.productIterator.foreach{
      case f: Field[_] => println(s"${f.name}: ${f.value}")
    }
  }

}
