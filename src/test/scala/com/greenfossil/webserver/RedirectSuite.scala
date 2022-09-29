package com.greenfossil.webserver

class RedirectSuite extends munit.FunSuite {

  //FIXME - need to put in place assertion
  test("redirect with query string"){
    val queryStringMap = Map("queryArg1" -> Seq("Hello World"), "queryArg2" -> Seq("Cat", "Dog"))
    val result = Redirect("/query-string", queryStringMap)
    println(s"result = ${result}")

  }

}
