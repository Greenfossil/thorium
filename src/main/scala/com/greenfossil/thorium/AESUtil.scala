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

import java.security.*
import java.security.spec.AlgorithmParameterSpec
import java.util.Base64
import javax.crypto.*
import javax.crypto.spec.{GCMParameterSpec, IvParameterSpec, PBEKeySpec, SecretKeySpec}

object AESUtil:

  val AES = "AES"
  val AES_CTR_NOPADDING = "AES/CTR/NoPadding"
  val AES_GCM_NOPADDING = "AES/GCM/NoPadding"

  val GCM_TAG_LENGTH = 16
  val IV_LENGTH = 16

  /**
   * Generate an AES key
   * @param n - number of bits, only allow 128, 192 and 256 bits
   * @return
   */
  def generateKey(n: 128|192|256): SecretKey =
    val gen = KeyGenerator.getInstance(AES)
    gen.init(n)
    gen.generateKey()

  def generateKeyPair(algorithm: String, n: Int): KeyPair =
    val keygen = KeyPairGenerator.getInstance(algorithm)
    keygen.initialize(n)
    keygen.genKeyPair()

  /**
   * Get a salted Key from a password
   * @param password
   * @param salt
   * @return
   */
  def getSaltedKeyFromPassword(password: String, salt: String): SecretKeySpec =
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val keySpec = PBEKeySpec(password.toCharArray, salt.getBytes(), 65536, 256)
    SecretKeySpec(factory.generateSecret(keySpec).getEncoded, AES)

  /**
   * Note: the Cipher key length is observed when creating the SecretKeySpec
   *
   * Generate a SecretKeySpec that will match a cipher key length requirement
   *
   * @param privateKey - can be any length
   * @param algorithm
   * @return
   */
  private def generateDerivedSecretKey(privateKey: String): SecretKeySpec =
    import java.security.MessageDigest
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(privateKey.getBytes())
    // max allowed length in bits / (8 bits to a byte)
    val maxAllowedKeyLength = Cipher.getMaxAllowedKeyLength(AES) / 8
    val derivedKey = messageDigest.digest().take(maxAllowedKeyLength)
    SecretKeySpec(derivedKey, AES)

  /**
   * Generate a 16 bytes IV
   * @return
   */
  def generateIV: IvParameterSpec =
    val iv = Array.ofDim[Byte](IV_LENGTH)
    SecureRandom().nextBytes(iv)
    IvParameterSpec(iv)

  def encryptWithEmbeddedIV(plainText: String, key: String, encoder: Base64.Encoder): String =
    encryptWithEmbeddedIV(plainText, key, AES_CTR_NOPADDING, encoder)

  def decryptWithEmbeddedIV(cipherText: String, key: String, decoder: Base64.Decoder): String =
    decryptWithEmbeddedIV(cipherText, key, AES_CTR_NOPADDING, decoder)


  /**
   * Encrypt plaintext with key
   * @param key
   * @param plainText
   * @return
   */
  def encryptWithEmbeddedIV(plainText: String, key: String, algorithm: String, encoder: Base64.Encoder): String =
    val secretKey = generateDerivedSecretKey(key)
    val iv = generateIV
    val payload = iv.getIV ++ encrypt(plainText, secretKey, algorithm, iv)
    encoder.encodeToString(payload)

  /**
   * Decrypt cipherText with key
   * @param key
   * @param cipherText
   * @return
   */
  def decryptWithEmbeddedIV(cipherText: String, key: String, algorithm: String, decoder: Base64.Decoder): String =
    val secretKey = generateDerivedSecretKey(key)
    val cipherTextBytes = decoder.decode(cipherText)
    val iv = IvParameterSpec(cipherTextBytes.take(IV_LENGTH))
    val bytes = decrypt(cipherTextBytes.drop(IV_LENGTH), secretKey, algorithm, iv)
    new String(bytes)

  def encrypt(plainText: String, key: SecretKey, algorithm: String, iv: IvParameterSpec, encoder: Base64.Encoder): String =
    encoder.encodeToString(encrypt(plainText, key, algorithm, iv))

  def decrypt(cipherText: String, key: SecretKey, algorithm: String, iv: IvParameterSpec, decoder: Base64.Decoder): String =
    new String(decrypt(decoder.decode(cipherText), key, algorithm, iv))

  private def getParamSpec(algorithm: String, iv: IvParameterSpec): AlgorithmParameterSpec =
    if algorithm.startsWith("AES/GCM")
    then GCMParameterSpec(GCM_TAG_LENGTH * 8, iv.getIV)
    else iv

  def encrypt(plainText: String, key: SecretKey, algorithm: String, iv: IvParameterSpec): Array[Byte] =
    val cipher = Cipher.getInstance(algorithm)
    val paramSpec = getParamSpec(algorithm, iv)
    cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec)
    cipher.doFinal(plainText.getBytes)

  def decrypt(bytes: Array[Byte], key: SecretKey, algorithm: String, iv: IvParameterSpec): Array[Byte] =
    val cipher = Cipher.getInstance(algorithm)
    val paramSpec = getParamSpec(algorithm, iv)
    cipher.init(Cipher.DECRYPT_MODE, key, paramSpec)
    cipher.doFinal(bytes)

  def encryptObject(obj: Serializable, key: SecretKey, algorithm: String, iv: IvParameterSpec): SealedObject =
    val cipher = Cipher.getInstance(algorithm)
    val paramSpec = getParamSpec(algorithm, iv)
    cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec)
    SealedObject(obj, cipher)

  def decryptObject[T](sealedObject: SealedObject, key: SecretKey, algorithm: String, iv: IvParameterSpec): Any =
    val cipher = Cipher.getInstance(algorithm)
    val paramSpec = getParamSpec(algorithm, iv)
    cipher.init(Cipher.DECRYPT_MODE, key, paramSpec)
    sealedObject.getObject(cipher).asInstanceOf[T]

  /**
   *
   * @param algorithm - DSA
   * @param obj
   * @return
   */
  def signObject(obj: Serializable, key: PrivateKey, algorithm: String): SignedObject =
    val signature = Signature.getInstance(algorithm)
    signature.initSign(key)
    SignedObject(obj, key, signature)

  def verifyObject(signedObj: SignedObject, publicKey: PublicKey, algorithm: String): Boolean =
    val signature = Signature.getInstance(algorithm)
    signedObj.verify(publicKey, signature)