package com.hash_net.beelinecrypto

import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

object CryptoService {
    private const val ALGORITHM = "RSA/ECB/OAEPPadding"
    private const val KEY_ALGORITHM = "RSA"
    private const val KEY_SIZE = 2048
    
    private var keyPair: java.security.KeyPair? = null
    
    private fun getOaepParameterSpec(): OAEPParameterSpec {
        return OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
        )
    }
    
    fun initialize() {
        if (keyPair == null) {
            val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
            generator.initialize(KEY_SIZE, SecureRandom())
            keyPair = generator.generateKeyPair()
        }
    }
    
    fun generateKeyPair(): Pair<String, String> {
        initialize()
        val publicKey = Base64.getEncoder().encodeToString(keyPair!!.public.encoded)
        val privateKey = Base64.getEncoder().encodeToString(keyPair!!.private.encoded)
        return publicKey to privateKey
    }
    
    fun getPublicKey(): String {
        initialize()
        return Base64.getEncoder().encodeToString(keyPair!!.public.encoded)
    }
    
    fun encrypt(plaintext: String, publicKeyBase64: String): String? {
        return try {
            val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec) as PublicKey
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, getOaepParameterSpec())
            
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(ciphertext)
        } catch (e: Exception) {
            System.err.println("Encryption error: ${e::class.java.name}: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    fun decrypt(ciphertext: String): String? {
        return try {
            initialize()
            val privateKey = keyPair!!.private
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, privateKey, getOaepParameterSpec())
            
            val ciphertextBytes = Base64.getDecoder().decode(ciphertext)
            val plaintext = cipher.doFinal(ciphertextBytes)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            System.err.println("Decryption error: ${e::class.java.name}: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    fun decryptWithKey(ciphertext: String, privateKeyBase64: String): String? {
        return try {
            val keyBytes = Base64.getDecoder().decode(privateKeyBase64)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val privateKey = keyFactory.generatePrivate(keySpec) as PrivateKey
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, privateKey, getOaepParameterSpec())
            
            val ciphertextBytes = Base64.getDecoder().decode(ciphertext)
            val plaintext = cipher.doFinal(ciphertextBytes)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
