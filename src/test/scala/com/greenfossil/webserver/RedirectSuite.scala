package com.greenfossil.webserver

class RedirectSuite extends munit.FunSuite {
  test("redirect with query string"){
    val queryStringMap = Map("query" -> Seq("hello"))
    val result = Redirect("/query-string", queryStringMap)
    assertEquals(result.queryString, queryStringMap)
  }

}
