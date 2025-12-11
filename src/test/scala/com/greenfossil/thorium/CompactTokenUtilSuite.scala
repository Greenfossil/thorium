/*
 * Copyright 2025 - test
 */

package com.greenfossil.thorium

import munit.FunSuite
import java.time.Instant
import scala.util.{Failure, Success}
import scala.concurrent.duration._

class CompactTokenUtilSuite extends FunSuite {

  val passwordKey = "thorium"
  val otherKey = "notthorium"

  test("roundtrip token - happy path") {
    val value = "how are you doing?"
    val duration = 1.hour
    val notBefore = Instant.now().minusSeconds(1)

    val res = for {
      token <- TokenUtil.generateCompactToken(value, duration, notBefore, passwordKey, AESUtil.AES_GCM_NOPADDING)
      _ = println(s"length=${token.length} token = ${token} ")
      verified <- TokenUtil.verifyCompactToken(token, passwordKey, AESUtil.AES_GCM_NOPADDING)
    } yield verified

    res match {
      case Success(vt) =>
        println(s"vt = ${vt}")
        assertEquals(vt.value, value)
        assertEquals(vt.isExpired, false)
      case Failure(ex) => fail(s"expected success but failed", ex)
    }
  }

  test("roundtrip with special characters") {
    val value = "a\"b\\c-üöß-\uD83D\uDE00" // contains quotes, backslash and emoji escape
    val duration = 2.hours
    val notBefore = Instant.now().minusSeconds(1)

    val res = for {
      token <- TokenUtil.generateCompactToken(value, duration, notBefore, passwordKey, AESUtil.AES_GCM_NOPADDING)
      verified <- TokenUtil.verifyCompactToken(token, passwordKey, AESUtil.AES_GCM_NOPADDING)
    } yield verified

    res match {
      case Success(vt) => assertEquals(vt.value, value)
      case Failure(ex) => fail(s"expected success but failed with: $ex")
    }
  }

  test("verify fails with wrong key") {
    val value = "sensitive"
    val duration = 1.hour
    val notBefore = Instant.now().minusSeconds(1)

    val res = for {
      token <- TokenUtil.generateCompactToken(value, duration, notBefore, passwordKey, AESUtil.AES_GCM_NOPADDING)
      verified <- TokenUtil.verifyCompactToken(token, otherKey, AESUtil.AES_GCM_NOPADDING)
    } yield verified

    res match {
      case Failure(ex) => assertNoDiff(ex.getMessage, "Invalid token (authentication failed)")
      case _ => fail("expected failure")
    }
  }

  test("verify fails for tampered token") {
    val value = "tamper-test"
    val duration = 1.hour
    val notBefore = Instant.now().minusSeconds(1)

    val res = for {
      token <- TokenUtil.generateCompactToken(value, duration, notBefore, passwordKey, AESUtil.AES_GCM_NOPADDING)
      tampered = if (token.isEmpty) token + "x" else token.dropRight(1) + (if (token.last != 'a') 'a' else 'b')
      verified <- TokenUtil.verifyCompactToken(tampered, passwordKey, AESUtil.AES_GCM_NOPADDING)
    } yield verified

    res match {
      case Failure(ex) => assertNoDiff(ex.getMessage, "Invalid token (authentication failed)")
      case _ => fail("expected failure")
    }

  }

  test("token not yet valid (nbf) returns failure with nbf message") {
    val value = "future-user"
    val duration = 1.hour
    val notBefore = Instant.now().plusSeconds(3600) // 1 hour in future

    val res = for {
      token <- TokenUtil.generateCompactToken(value, duration, notBefore, passwordKey, AESUtil.AES_GCM_NOPADDING)
      verified <- TokenUtil.verifyCompactToken(token, passwordKey, AESUtil.AES_GCM_NOPADDING)
    } yield verified

    assert(res.isFailure)
    res match {
      case Failure(ex) => assertNoDiff(ex.getMessage, "Token not yet valid (nbf)")
      case _ => fail("expected failure")
    }
  }

  test("expired token is rejected") {
    val value = "expired"
    // create a token whose exp is in the past beyond the default clock skew (30s)
    val duration = Duration(-31, SECONDS)
    val notBefore = Instant.now().minusSeconds(3600)

    val res = for {
      token <- TokenUtil.generateCompactToken(value, duration, notBefore, passwordKey, AESUtil.AES_GCM_NOPADDING)
      verified <- TokenUtil.verifyCompactToken(token, passwordKey, AESUtil.AES_GCM_NOPADDING)
    } yield verified

    // expired tokens should verify successfully but be marked as expired
    res match {
      case Success(vt) =>
        assertEquals(vt.value, value)
        assertEquals(vt.isExpired, true)
      case Failure(ex) => fail(s"expected success but failed with: $ex")
    }
  }

  test("malformed token fails gracefully") {
    val bad = "not-a-valid-token-at-all"
    val res = TokenUtil.verifyCompactToken(bad, passwordKey, AESUtil.AES_GCM_NOPADDING)
    assert(res.isFailure)
  }

  test("mismatched algorithm fails verification") {
    val value = "algo-test"
    val duration = 1.hour
    val notBefore = Instant.now().minusSeconds(1)

    val res = for {
      token <- TokenUtil.generateCompactToken(value, duration, notBefore, passwordKey, AESUtil.AES_GCM_NOPADDING)
      verified <- TokenUtil.verifyCompactToken(token, passwordKey, AESUtil.AES_CTR_NOPADDING)
    } yield verified

    assert(res.isFailure)
  }

  test("null notBefore treats compact token as valid immediately") {
    val value = "null-nbf-compact"
    val duration = 1.hour

    val res = for {
      token <- TokenUtil.generateCompactToken(value, duration, null, passwordKey, AESUtil.AES_GCM_NOPADDING)
      verified <- TokenUtil.verifyCompactToken(token, passwordKey, AESUtil.AES_GCM_NOPADDING)
    } yield verified

    res match {
      case Success(vt) =>
        assertEquals(vt.value, value)
        assertEquals(vt.isExpired, false)
      case Failure(ex) => fail(s"expected success but failed: $ex")
    }
  }

}
