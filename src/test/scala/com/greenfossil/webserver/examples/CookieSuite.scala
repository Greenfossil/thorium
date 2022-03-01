package com.greenfossil.webserver.examples

import com.linecorp.armeria.common.{Cookie, HttpResponse, ResponseHeaders}

class CookieSuite extends munit.FunSuite {

  test("cookie"){
    val cookie = Cookie.ofSecure("name", "value")
    val resp = HttpResponse.of("response")
    println(s"resp = ${resp.peekHeaders(headers => {
      val c = headers.cookies()
      c.toString
    } )}")
    val cookieResp = resp.mapHeaders(headers => headers.toBuilder.cookie(cookie).build() )
    println(s"cookieResp = ${cookieResp.peekHeaders(headers => {
      val c = headers.cookies()
      c.toString
    })}")
  }

}
