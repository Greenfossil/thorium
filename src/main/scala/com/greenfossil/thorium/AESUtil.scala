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
    val iv = Array.ofDim[Byte](16)
    SecureRandom().nextBytes(iv)
    IvParameterSpec(iv)

  /**
   * Encrypt plaintext with key
   * @param key
   * @param plainText
   * @return
   */
  def encrypt(key: String, plainText: String, algorithm: String = AES_CTR_NOPADDING): String =
    val secretKey = generateDerivedSecretKey(key)
    val iv = generateIV
    val payload = iv.getIV ++ encrypt(algorithm, plainText, secretKey, iv)
    Base64.getEncoder.encodeToString(payload)

  /**
   * Decrypt cipherText with key
   * @param key
   * @param cipherText
   * @return
   */
  def decrypt(key: String, cipherText: String, algorithm: String = AES_CTR_NOPADDING) : String =
    val secretKey = generateDerivedSecretKey(key)
    val cipherTextBytes = Base64.getDecoder.decode(cipherText)
    val iv = IvParameterSpec(cipherTextBytes.take(16))
    val bytes = decrypt(algorithm, cipherTextBytes.drop(16), secretKey, iv)
    new String(bytes)

  def base64Encrypt(algorithm: String, input:String, key: SecretKey, iv: IvParameterSpec): String =
    val bytes = encrypt(algorithm, input, key, iv)
    Base64.getEncoder.encodeToString(bytes)

  def base64Decrypt(algorithm: String, cipherText: String, key: SecretKey, iv: IvParameterSpec): String =
    val bytes = decrypt(algorithm, Base64.getDecoder.decode(cipherText), key, iv)
    new String(bytes)

  private def getParamSpec(algorithm: String, iv: IvParameterSpec): AlgorithmParameterSpec =
    if algorithm.startsWith("AES/GCM")
    then GCMParameterSpec(GCM_TAG_LENGTH * 8, iv.getIV)
    else iv

  def encrypt(algorithm: String, input: String, key: SecretKey, iv: IvParameterSpec): Array[Byte] =
    val cipher = Cipher.getInstance(algorithm)
    val paramSpec = getParamSpec(algorithm, iv)
    cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec)
    cipher.doFinal(input.getBytes)

  def decrypt(algorithm: String, bytes: Array[Byte], key: SecretKey, iv: IvParameterSpec): Array[Byte] =
    val cipher = Cipher.getInstance(algorithm)
    val paramSpec = getParamSpec(algorithm, iv)
    cipher.init(Cipher.DECRYPT_MODE, key, paramSpec)
    cipher.doFinal(bytes)

  def encryptObject(algorithm: String, obj: Serializable, key: SecretKey, iv: IvParameterSpec): SealedObject =
    val cipher = Cipher.getInstance(algorithm)
    val paramSpec = getParamSpec(algorithm, iv)
    cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec)
    SealedObject(obj, cipher)

  def decryptObject[T](algorithm: String,  sealedObject: SealedObject, key: SecretKey, iv: IvParameterSpec): T =
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
  def signObject(algorithm: String, obj: Serializable, key: PrivateKey): SignedObject =
    val signature = Signature.getInstance(algorithm)
    signature.initSign(key)
    SignedObject(obj, key, signature)

  def verifyObject(algorithm: String, signedObj: SignedObject, publicKey: PublicKey): Boolean =
    val signature = Signature.getInstance(algorithm)
    signedObj.verify(publicKey, signature)