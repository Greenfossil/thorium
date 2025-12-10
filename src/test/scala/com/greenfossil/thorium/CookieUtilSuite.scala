package com.greenfossil.thorium

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.Cookie
import munit.FunSuite

import java.nio.charset.StandardCharsets
import java.util.Base64

class CookieUtilSuite extends FunSuite:

  test("Cookie.toJson should decode a base64-url encoded cookie value") {
    val js = Json.obj("foo" -> "bar", "n" -> 1)
    val bytes = js.stringify.getBytes(StandardCharsets.UTF_8)
    val encoded = Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
    val cookie = Cookie.ofSecure("c", encoded)
    assertEquals(cookie.toJson, js)
    assertEquals(CookieUtil.cookiesToJson(Seq(cookie)), js)
  }