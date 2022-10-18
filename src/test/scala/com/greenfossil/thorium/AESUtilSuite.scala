package com.greenfossil.thorium

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.security.KeyPairGenerator

@SerialVersionUID(1L)
case class User(id: Long, name: String) extends Serializable

class AESUtilSuite extends munit.FunSuite {

  test("Encrypt then Decrypt using generateKey") {
    val input = "thorium 钍"
    val key =  AESUtil.generateKey(128)
    val ivSpec = AESUtil.generateIV
    val algo = "AES/CBC/PKCS5Padding"
    val cipherText = AESUtil.base64Encrypt(algo, input, key, ivSpec)
    val plainText =  AESUtil.base64Decrypt(algo, cipherText, key, ivSpec)
    assertNoDiff(plainText, input)
  }

  test("Encrypt then decrypt using Password"){
    val input = "Thorium 钍"
    val password = "thorium"
    val salt = "12345678"
    val key = AESUtil.getSaltedKeyFromPassword(password, salt)
    val ivSpec = AESUtil.generateIV
    val algo = "AES/CBC/PKCS5Padding"
    val cipherText = AESUtil.base64Encrypt(algo, input, key, ivSpec)
    val plainText = AESUtil.base64Decrypt(algo, cipherText, key, ivSpec)
    assertNoDiff(plainText, input)
  }

  test("Encrypt then decrypt of an object") {
    val homer = User(42, "Homer")
    val key = AESUtil.generateKey(128)
    val iv = AESUtil.generateIV
    val algo = "AES/CBC/PKCS5Padding"
    val sealedObject = AESUtil.encryptObject(algo, homer, key, iv)
    val obj = AESUtil.decryptObject[User](algo, sealedObject, key, iv)
    assertEquals(obj, homer)
  }

  test("Sign and verify object") {
    val homer = User(42, "Homer")
    val algo = "DSA"
    val keyPair = AESUtil.generateKeyPair(algo, 1024)
    val signedObj =  AESUtil.signObject(algo, homer, keyPair.getPrivate)
    assertEquals(AESUtil.verifyObject(algo, signedObj, keyPair.getPublic), true)
  }

  test("encrypt/decrypt with Derived Key") {
    val input = "Thorium 钍"
    val key = "thorium"
    val cipherText = AESUtil.encrypt(key, input)
    val plainText = AESUtil.decrypt(key, cipherText)
    assertNoDiff(plainText, input)
  }

}
