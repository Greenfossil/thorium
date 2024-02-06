package com.greenfossil.thorium

import com.greenfossil.commons.json.Json

class RecaptchaSuite extends munit.FunSuite {

  test("GoogleRecaptchaUtil.siteVerify"){
    val v2SecretKey = ""
    Recaptcha.siteVerify("bad-token", v2SecretKey, 1000)
      .fold(
        ex => fail("Request should succeed", ex),
        recaptchaResponse =>
          assertNoDiff(recaptchaResponse.jsValue.stringify, """{"success":false,"error-codes":["invalid-input-secret"]}""")
      )
  }

  test("V2 parser"){
    val json = """{"success":true,"challenge_ts":"2024-01-30T09:22:27Z","hostname":"localhost","action":"submit"}"""
    val r = Recaptcha(Json.parse(json))
    assertEquals(r.success, true)
    assertEquals(r.scoreOpt, None)
    assertEquals(r.actionOpt, Option("submit"))
    assertEquals(r.challengeTS, Option(java.time.Instant.parse("2024-01-30T09:22:27Z")))
    assertNoDiff(r.hostname, "localhost")
  }

  test("V3 parser") {
    val json = """{"success":true,"challenge_ts":"2024-01-30T09:39:46Z","hostname":"localhost","score":0.9,"action":"submit"}"""
    val r = Recaptcha(Json.parse(json))
    assertEquals(r.success, true)
    assertEquals(r.scoreOpt, Option(0.9))
    assertEquals(r.actionOpt, Option("submit"))
    assertEquals(r.challengeTS, Option(java.time.Instant.parse("2024-01-30T09:39:46Z")))
    assertNoDiff(r.hostname, "localhost")
  }

}
