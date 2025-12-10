package com.greenfossil.thorium

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.time.{Instant, Duration as _}
import java.util.{Base64, UUID}
import scala.util.{Failure, Random, Success, Try}
import scala.concurrent.duration.Duration

final case class VerifiedToken(
  value: String,
  jti: String,
  issuedAt: Instant,
  notBefore: Instant,
  expiresAt: Instant,
  isExpired: Boolean
)

object TokenUtil:

  private val DefaultClockSkewSeconds = 30L

  private def escape(s: String): String =
    s.replace("\\", "\\\\").replace("\"", "\\\"")

  private def unescape(s: String): String =
    s.replace("\\\"", "\"").replace("\\\\", "\\")

  /**
   *
   * @param value
   * @param duration
   * @param notBefore
   * @param key
   * @return
   */
  def generateToken(value: String, duration: Duration, notBefore: Instant, key: String): Try[String] =
    generateToken(value, duration, notBefore, key, AESUtil.AES_GCM_NOPADDING)

  /**
   * Generate an authenticated token (AES-GCM + embedded IV + base64url without padding).
   * Does not modify existing AESUtil; calls its encryptWithEmbeddedIV overload that accepts algorithm.
   *
   * @param value - opaque value to include
   * @param duration - token lifetime
   * @param notBefore - when token becomes valid
   * @return Try of URL-safe base64 token string
   */
  def generateToken(value: String, duration: Duration, notBefore: Instant, key: String, algo: String, aad: String = ""): Try[String] =
    Try {
      val encoder = Base64.getUrlEncoder.withoutPadding()
      val issued = Instant.now()
      val jti = UUID.randomUUID().toString
      val exp = issued.plusMillis(duration.toMillis)

      // compact JSON; avoid depending on external JSON libs to keep changes minimal
      val payload =
        s"""{"ver":1,"jti":"${escape(jti)}","v":"${encoder.encodeToString(value.getBytes())}","iat":${issued.toEpochMilli},"nbf":${notBefore.toEpochMilli},"exp":${exp.toEpochMilli}}"""

      // use AES-GCM for authenticated encryption
      AESUtil.encryptWithEmbeddedIV(payload, key, algo, aad, encoder)
    }


  def verifyToken(token: String, key: String): Try[VerifiedToken] =
    verifyToken(token, key, AESUtil.AES_GCM_NOPADDING)

  /**
   * Verify token created by generateToken.
   * Validates nbf/exp with a small clock skew tolerance and returns a typed result.
   *
   * @param token - token string (base64url without padding)
   * @return Try[VerifiedToken] or Failure with reason
   */
  def verifyToken(token: String, key: String, algo: String, aad: String = ""): Try[VerifiedToken] =
    Try {
      val decoder = Base64.getUrlDecoder

      val json = AESUtil.decryptWithEmbeddedIV(token, key, algo, aad, decoder)

      // extract fields individually to be robust against ordering
      def findStrField(name: String): Option[String] =
        val pat = ("\"" + name + "\":\"([^\"]*)\"").r
        pat.findFirstMatchIn(json).map(_.group(1))

      def findLongField(name: String): Option[Long] =
        val pat = ("\"" + name + "\":(\\d+)").r
        pat.findFirstMatchIn(json).map(_.group(1).toLong)

      val verOpt = findLongField("ver")
      val jtiOpt = findStrField("jti")
      val vOpt = findStrField("v").map(s => new String(decoder.decode(s)))
      val iatOpt = findLongField("iat")
      val nbfOpt = findLongField("nbf")
      val expOpt = findLongField("exp")

      (verOpt, jtiOpt, vOpt, iatOpt, nbfOpt, expOpt) match
        case (Some(_ver), Some(jti), Some(rawV), Some(iatMs), Some(nbfMs), Some(expMs)) =>
          val issued = Instant.ofEpochMilli(iatMs)
          val notBefore = Instant.ofEpochMilli(nbfMs)
          val expiresAt = Instant.ofEpochMilli(expMs)
          val now = Instant.now()
          val skew = java.time.Duration.ofSeconds(DefaultClockSkewSeconds)

          if (now.plus(skew).isBefore(notBefore)) throw new IllegalStateException("Token not yet valid (nbf)")
          // Do not throw on expiry; instead return the token with isExpired flag set.
          // We consider the token expired when it's past the allowed clock skew.
          val isExpired = now.minus(skew).isAfter(expiresAt) || now.isAfter(expiresAt)
          VerifiedToken(unescape(rawV), unescape(jti), issued, notBefore, expiresAt, isExpired)
        case _ =>
          throw new IllegalArgumentException("Invalid token payload")
    }.recoverWith {
      case ex: IllegalStateException => Failure(ex)
      case ex: IllegalArgumentException => Failure(ex)
      case ex => Failure(new RuntimeException("Token verification failed"))
    }

  private val rnd = new Random()

  private def epochSec(i: Instant): Int = Math.toIntExact(i.getEpochSecond)

  def generateCompactToken(value: String, duration: Duration, notBefore: Instant, key: String): Try[String] =
    generateCompactToken(value, duration, notBefore, key, AESUtil.AES_GCM_NOPADDING)

  /** Generate compact binary token then AEAD-encrypt and base64url encode without padding. */
  def generateCompactToken(value: String, duration: Duration, notBefore: Instant, key: String, algo: String): Try[String] =
    Try {
      val issued = Instant.now()
      val exp = issued.plusMillis(duration.toMillis)

      // build compact binary payload
      val baos = new ByteArrayOutputStream()
      val dos = new DataOutputStream(baos)

      // version
      dos.writeByte(1)

      // jti: 8 random bytes (shorter than UUID)
      val jti = new Array[Byte](8)
      rnd.nextBytes(jti)
      dos.write(jti)

      // times as 32-bit epoch seconds
      dos.writeInt(epochSec(issued))
      dos.writeInt(epochSec(notBefore))
      dos.writeInt(epochSec(exp))

      // value bytes prefixed by 32-bit length
      val encoder = Base64.getUrlEncoder.withoutPadding()
      // use UTF-8 for proper emoji and Unicode support
      val vbytes = encoder.encode(value.getBytes("UTF-8"))
      dos.writeInt(vbytes.length)
      dos.write(vbytes)

      dos.flush()

      // AESUtil currently exposes String-based encrypt/decrypt helpers; to preserve raw bytes we
      // convert the payload bytes to an ISO-8859-1 String (1:1 byte->char) so the exact bytes are
      // preserved through AESUtil's String overloads. The value inside the payload is UTF-8
      // base64-encoded, so it will survive this round-trip and support emoji.
      val payloadStr = new String(baos.toByteArray(), "ISO-8859-1")
      AESUtil.encryptWithEmbeddedIV(payloadStr, key, algo, encoder)
    }

  def verifyCompactToken(token: String, key: String): Try[VerifiedToken] =
    verifyCompactToken(token, key, AESUtil.AES_GCM_NOPADDING)

  /** Verify compact token created by generateCompactToken. */
  def verifyCompactToken(token: String, key: String, algo: String): Try[VerifiedToken] =
    Try {
      val decoder = Base64.getUrlDecoder
      val jsonOrBytes = AESUtil.decryptWithEmbeddedIV(token, key, algo, decoder)
      // AESUtil may return String for previous API; handle both String and binary bytes gracefully:
      val bytes: Array[Byte] = jsonOrBytes match {
        case s: String => s.getBytes("ISO-8859-1") // if AESUtil returned String of raw bytes
        case null => throw new IllegalArgumentException("Unexpected decrypted payload type")
      }

      val dis = new DataInputStream(new ByteArrayInputStream(bytes))

      val ver = dis.readUnsignedByte()
      if (ver != 1) throw new IllegalArgumentException("unsupported token version")

      val jtiBytes = new Array[Byte](8)
      dis.readFully(jtiBytes)
      val jti = jtiBytes.map("%02x".format(_)).mkString

      val iat = Instant.ofEpochSecond(dis.readInt().toLong)
      val nbf = Instant.ofEpochSecond(dis.readInt().toLong)
      val exp = Instant.ofEpochSecond(dis.readInt().toLong)

      val vlen = dis.readInt()
      if (vlen < 0 || vlen > 10 * 1024 * 1024) throw new IllegalArgumentException("invalid value length")
      val vbytes = new Array[Byte](vlen)
      dis.readFully(vbytes)
      // the value bytes were base64-url-encoded from UTF-8 bytes; decode and then convert to UTF-8
      val value = new String(decoder.decode(vbytes), "UTF-8")

      val now = Instant.now()
      val skew = java.time.Duration.ofSeconds(DefaultClockSkewSeconds)

      if (now.plus(skew).isBefore(nbf)) throw new IllegalStateException("Token not yet valid (nbf)")
      // Do not throw on expiry; instead return the token with isExpired flag set.
      // Consider token expired when it's past the allowed clock skew.
      val isExpired = now.minus(skew).isAfter(exp) || now.isAfter(exp)
      VerifiedToken(value, jti, iat, nbf, exp, isExpired)
    }.recoverWith {
      case ex: IllegalStateException => Failure(ex)
      case ex: IllegalArgumentException => Failure(ex)
      case ex => Failure(new RuntimeException("Token verification failed", ex))
    }