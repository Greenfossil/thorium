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

import java.util.Base64

@SerialVersionUID(1L)
case class User(id: Long, name: String) extends Serializable

class AESUtilSuite extends munit.FunSuite {

  val ALGO = AESUtil.AES_GCM_NOPADDING

  test("Base64 Encrypt then Decrypt using generateKey") {
    val text = "thorium 钍"
    val key =  AESUtil.generateKey(128)
    val ivSpec = AESUtil.generateIV
    val algo = ALGO
    val cipherText = AESUtil.encrypt(text, key, algo, ivSpec, Base64.getEncoder)
    val plainText =  AESUtil.decrypt(cipherText, key, algo, ivSpec, Base64.getDecoder)
    assertNoDiff(plainText, text)
  }

  test("Base64 Encrypt then decrypt using Password"){
    val text = "Thorium 钍"
    val password = "thorium"
    val salt = "12345678"
    val key = AESUtil.getSaltedKeyFromPassword(password, salt)
    val ivSpec = AESUtil.generateIV
    val algo = ALGO
    val cipherText = AESUtil.encrypt(text, key, algo, ivSpec, Base64.getEncoder)
    val plainText = AESUtil.decrypt(cipherText, key, algo, ivSpec, Base64.getDecoder)
    assertNoDiff(plainText, text)
  }

  test("URLBase64 Encrypt then Decrypt using generateKey") {
    val text = "thorium 钍"
    val key = AESUtil.generateKey(128)
    val ivSpec = AESUtil.generateIV
    val algo = ALGO
    val cipherText = AESUtil.encrypt(text, key, algo, ivSpec, Base64.getUrlEncoder)
    val plainText = AESUtil.decrypt(cipherText, key, algo, ivSpec, Base64.getUrlDecoder)
    assertNoDiff(plainText, text)
  }

  test("URLBase64 Encrypt then decrypt using Password") {
    val input = "Thorium 钍"
    val password = "thorium"
    val salt = "12345678"
    val key = AESUtil.getSaltedKeyFromPassword(password, salt)
    val ivSpec = AESUtil.generateIV
    val algo = ALGO
    val cipherText = AESUtil.encrypt(input, key, algo, ivSpec, Base64.getUrlEncoder)
    val plainText = AESUtil.decrypt(cipherText, key, algo, ivSpec, Base64.getUrlDecoder)
    assertNoDiff(plainText, input)
  }

  test("Encrypt then decrypt of an object") {
    val homer = User(42, "Homer")
    val key = AESUtil.generateKey(128)
    val iv = AESUtil.generateIV
    val algo = ALGO
    val sealedObject = AESUtil.encryptObject(homer, key, algo, iv)
    val obj = AESUtil.decryptObject[User](sealedObject, key, algo, iv)
    assertEquals(obj, homer)
  }

  test("Sign and verify object") {
    val homer = User(42, "Homer")
    val algo = "DSA"
    val keyPair = AESUtil.generateKeyPair(algo, 1024)
    val signedObj =  AESUtil.signObject(homer, keyPair.getPrivate, algo)
    assertEquals(AESUtil.verifyObject(signedObj, keyPair.getPublic, algo), true)
  }

  test("encrypt/decrypt with Derived Key") {
    val input = "Thorium 钍"
    val key = "thorium"
    val cipherText = AESUtil.encryptWithEmbeddedIV(input, key, Base64.getEncoder)
    val plainText = AESUtil.decryptWithEmbeddedIV(cipherText, key, Base64.getDecoder)
    assertNoDiff(plainText, input)
  }

  test("encrypt/decrypt with Derived Key explicit algo") {
    val input = "Thorium 钍"
    val key = "thorium"
    val cipherText = AESUtil.encryptWithEmbeddedIV(input, key, AESUtil.AES_GCM_NOPADDING, Base64.getEncoder)
    val plainText = AESUtil.decryptWithEmbeddedIV(cipherText, key, AESUtil.AES_GCM_NOPADDING, Base64.getDecoder)
    assertNoDiff(plainText, input)
  }

  test("SecretKey to Base64 and back conversion") {
    // Generate a 256-bit key
    val originalKey = AESUtil.generateKey(256)

    // Convert SecretKey to Base64
    val base64Key: String = AESUtil.secretKeyToBase64(originalKey, Base64.getEncoder)

    // Convert Base64 back to SecretKey
    val decodedKey = AESUtil.base64ToSecretKey(base64Key, Base64.getDecoder)

    // Verify the conversion
    assertEquals(originalKey.getEncoded.toSeq, decodedKey.getEncoded.toSeq)
  }
  
  test("generateBase64Key") {
    val base64Key = AESUtil.generateBase64Key(256, Base64.getEncoder)
    println(s"base64Key.length = ${base64Key.length}")
    println(s"base64Key = ${base64Key}")
  }

  test("Base64Encoding with and without padding"){
    val bytes = "".getBytes
    val paddingEncode = Base64.getEncoder.encodeToString(bytes)
    val withoutPaddingEncode = Base64.getEncoder.withoutPadding().encodeToString(bytes)
    println(s"paddingEncode = ${paddingEncode} len:${paddingEncode.length}")
    println(s"withoutPadding = ${withoutPaddingEncode} len:${withoutPaddingEncode.length}")
  }

}
