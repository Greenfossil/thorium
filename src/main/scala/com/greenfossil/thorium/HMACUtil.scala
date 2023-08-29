/*
 * Copyright 2022 Greenfossil Pte Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greenfossil.thorium

import java.security.{MessageDigest, SecureRandom}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HMACUtil:

  /**
   *
   * @param message
   * @param key       -
   * @param algorithm - HmacSHA256, HmacSHA384, HmacSHA512 or the equivalent HS256, HS384, HS512
   * @param converter
   * @tparam A
   * @return
   */
  def hmac[A](message: Array[Byte], key: Array[Byte], algorithm: String, converter: Array[Byte] => A): A =
    val alg = algorithm.replaceAll("^HS", "HmacSHA")
    val keySpec = new SecretKeySpec(key, alg)
    val mac = Mac.getInstance(alg)
    mac.init(keySpec)
    converter(mac.doFinal(message))

  def digest(bytes: Array[Byte], algorithm: String): Array[Byte] =
    digest(bytes, algorithm, identity)

  def digest[A](bytes: Array[Byte], algorithm: String, converter: Array[Byte] => A): A =
    digest(Seq(bytes), algorithm, converter)

  def digest[A](bytesSeq: Seq[Array[Byte]], algorithm: String, converter: Array[Byte] => A): A =
    val md = MessageDigest.getInstance(algorithm)
    bytesSeq.foreach(v => md.update(v))
    converter(md.digest)

  /**
   *
   * @param byteLength
   * @return
   */
  def randomBytes(byteLength: Int): Array[Byte] =
    randomBytes(byteLength, "NativePRNG", identity)

  def randomBytes[A](byteLength: Int, converter: Array[Byte] => A): A =
    randomBytes(byteLength, "NativePRNG", converter)


  /**
   *
   * @param byteLength
   * @param algorithm - e.g. NativePRNG
   * @return
   */
  def randomBytes[A](byteLength: Int, algorithm: String, converter: Array[Byte] => A): A =
    val random = SecureRandom.getInstance(algorithm)
    val bytes = new Array[Byte](byteLength)
    random.nextBytes(bytes)
    converter(bytes)

  /**
   *
   * @param bytes
   * @return
   */
  def hex(bytes: Array[Byte]): String =
    bytes.map(byte => f"${byte & 0xFF}%02x").mkString("")

  /**
   *
   * @param length
   * @return
   */
  def randomAlphaNumericString(length: Int): String =
    randomAlphaNumericString(length, "NativePRNG")

  inline private val ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
  inline private val NUMERIC_STRING = "0123456789"

  /**
   *
   * @param length
   * @param algorithm
   * @return
   */
  def randomAlphaNumericString(length: Int, algorithm: String): String =
    randomStringBuilder(ALPHA_NUMERIC_STRING, length, algorithm)

  /**
   *
   * @param length
   * @return
   */
  def randomNumericString(length: Int): String =
    randomNumericString(length, "NativePRNG")

  /**
   * Generate a random numeric string
   *
   * @param length
   * @param algorithm
   * @return
   */
  def randomNumericString(length: Int, algorithm: String): String =
    randomStringBuilder(NUMERIC_STRING, length, algorithm)

  /**
   *
   * @param str
   * @param length
   * @param rand
   * @return
   */
  private def randomStringBuilder(str: String, length: Int, algorithm: String): String =
    if length < 1 then
      throw new IllegalArgumentException("The length must be a positive integer")

    val random = SecureRandom.getInstance(algorithm)
    val alphaLength = str.length
    val builder = new StringBuilder()
    0 until length foreach { i =>
      builder.append(str.charAt(random.nextInt(alphaLength)))
    }
    builder.toString


  /**
   * Constant time equals method.
   *
   * Given a length that both Arrays are equal to, this method will always run in constant time.
   * This prevents timing attacks.
   */
  def constantTimeEquals(array1: Array[Byte], array2: Array[Byte]): Boolean =
  // Check if the arrays have the same length
    if array1.length != array2.length then false
    else
      // Perform a bitwise comparison of each byte
      // If the arrays are equal, the result will be 0
      var result = 0
      for (i <- array1.indices) {
        result |= array1(i) ^ array2(i)
      }
      result == 0