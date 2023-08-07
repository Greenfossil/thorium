package com.greenfossil.thorium

import com.greenfossil.thorium.decorators.CSRFProtectionDecoratingFunction

class CSRFProtectionDecoratingFunction1Suite extends munit.FunSuite:

  test("csrfToken - hmac with different key") {
    (for {
      csrfToken <- CSRFProtectionDecoratingFunction.generateCSRFToken("ABCD", "HmacSHA256", "1234")
    } yield
      val verifyResult = CSRFProtectionDecoratingFunction.verifyHmac(csrfToken, "ABCD1", "HmacSHA256")
      println(s"csrfToken = ${csrfToken}")
      println(s"verifyResult = ${verifyResult}")
      verifyResult
      )
      .fold(
        ex => fail("Fail", ex),
        success => assert(!success)
      )
  }

  test("csrfToken - with same key") {
    (for {
      csrfToken <- CSRFProtectionDecoratingFunction.generateCSRFToken("ABCD", "HmacSHA256", "1234")
    } yield
      val verifyResult = CSRFProtectionDecoratingFunction.verifyHmac(csrfToken, "ABCD", "HmacSHA256")
      println(s"csrfToken = ${csrfToken}")
      println(s"verifyResult = ${verifyResult}")
      verifyResult
      )
      .fold(
        ex => fail("Fail", ex),
        success => assert(success)
      )
  }


