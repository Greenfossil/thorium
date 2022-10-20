package com.greenfossil.thorium

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.security.KeyPairGenerator
import java.util.Base64
import javax.crypto.spec.IvParameterSpec
import javax.crypto.{SealedObject, SecretKey}

@SerialVersionUID(1L)
case class User(id: Long, name: String) extends Serializable

class AESUtilSuite extends munit.FunSuite {

  val ALGO = AESUtil.AES_CTR_NOPADDING

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

  test("encrypt/decrypt with Derived Key") {
    val input = "Thorium 钍"
    val key = "thorium"
    val cipherText = AESUtil.encryptWithEmbeddedIV(input, key, AESUtil.AES_GCM_NOPADDING, Base64.getEncoder)
    val plainText = AESUtil.decryptWithEmbeddedIV(cipherText, key, AESUtil.AES_GCM_NOPADDING, Base64.getDecoder)
    assertNoDiff(plainText, input)
  }

}
