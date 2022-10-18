package com.greenfossil.thorium

import java.security.*
import java.util.Base64
import javax.crypto.*
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}

object AESUtil:

  val AES = "AES"

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
  def encrypt(key: String, plainText: String): String =
    val secretKey = generateDerivedSecretKey(key)
    val iv = generateIV
    val payload = iv.getIV ++ encrypt("AES/CTR/NoPadding", plainText, secretKey, iv)
    Base64.getEncoder.encodeToString(payload)

  /**
   * Decrypt cipherText with key
   * @param key
   * @param cipherText
   * @return
   */
  def decrypt(key: String, cipherText: String) : String =
    val secretKey = generateDerivedSecretKey(key)
    val cipherTextBytes = Base64.getDecoder.decode(cipherText)
    val iv = IvParameterSpec(cipherTextBytes.take(16))
    val bytes = decrypt("AES/CTR/NoPadding", cipherTextBytes.drop(16), secretKey, iv)
    new String(bytes)

  def base64Encrypt(algorithm: String, input:String, key: SecretKey, iv: IvParameterSpec): String =
    val bytes = encrypt(algorithm, input, key, iv)
    Base64.getEncoder.encodeToString(bytes)

  def base64Decrypt(algorithm: String, cipherText: String, key: SecretKey, iv: IvParameterSpec): String =
    val bytes = decrypt(algorithm, Base64.getDecoder.decode(cipherText), key, iv)
    new String(bytes)

  def encrypt(algorithm: String, input: String, key: SecretKey, iv: IvParameterSpec): Array[Byte] =
    val cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    cipher.doFinal(input.getBytes)

  def decrypt(algorithm: String, bytes: Array[Byte], key: SecretKey, iv: IvParameterSpec): Array[Byte] =
    val cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    cipher.doFinal(bytes)

  def encryptObject(algorithm: String, obj: Serializable, key: SecretKey, iv: IvParameterSpec): SealedObject =
    val cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    SealedObject(obj, cipher)

  def decryptObject[T](algorithm: String,  sealedObject: SealedObject, key: SecretKey, iv: IvParameterSpec): T =
    val cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
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