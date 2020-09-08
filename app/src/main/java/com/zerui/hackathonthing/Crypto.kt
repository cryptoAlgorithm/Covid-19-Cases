package com.zerui.hackathonthing

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    fun encrypt(plainText: String, password: String): HashMap<String, ByteArray> {
        // Salt generation
        val random = SecureRandom()
        val salt = ByteArray(256)
        random.nextBytes(salt) // Fill the salt with secure randomBytes

        // Key generation
        val pbKeySpec = PBEKeySpec(
            password.toCharArray(),
            salt,
            1324, // Higher numbers increases brute-force resistance
            256
        ) // Get password-based encryption object
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded
        val keySpec = SecretKeySpec(keyBytes, "AES")

        // IV generation
        val randomIV = SecureRandom() // For security don't use previous instance
        val iv = ByteArray(16) // 16 Bytes for AES-256
        randomIV.nextBytes(iv)
        val ivSpec = IvParameterSpec(iv) // Get IV object

        // Where the magic happens...
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding") // Pad the remaining space in plaintext
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec) // Setup cipher object
        val encryptedText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Package everything into a HashMap
        var map: HashMap<String, ByteArray> = HashMap()
        map["salt"] = salt
        map["iv"] = iv
        map["encrypted"] = encryptedText

        return map
    }

    fun decrypt(map: HashMap<String, ByteArray>, password: String): ByteArray {
        // Get values from the Map object
        val salt = map["salt"]
        val iv = map["iv"]
        val encrypted = map["encrypted"]

        val pbKeySpec = PBEKeySpec(
            password.toCharArray(),
            salt,
            1324,
            256
        )
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded
        val keySpec = SecretKeySpec(keyBytes, "AES")

        // Actually decrypt encrypted message
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return try {
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
            ByteArray(0)
        }
    }
}