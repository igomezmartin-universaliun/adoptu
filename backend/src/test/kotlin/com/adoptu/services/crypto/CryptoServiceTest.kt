package com.adoptu.services.crypto

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertNotEquals

class CryptoServiceTest {

    @Test
    fun `generateKeyPair returns a usable public and private key pair`() {
        val (publicKey, privateKey) = CryptoService.generateKeyPair()

        assertNotNull(publicKey)
        assertNotNull(privateKey)
        assertNotEquals(publicKey, privateKey)
    }

    @Test
    fun `generateKeyPair output can encrypt and decryptWithKey can decrypt`() {
        val (publicKey, privateKey) = CryptoService.generateKeyPair()

        val ciphertext = CryptoService.encrypt("hello world", publicKey)
        assertNotNull(ciphertext)

        val plaintext = CryptoService.decryptWithKey(ciphertext, privateKey)
        assertEquals("hello world", plaintext)
    }

    @Test
    fun `decryptWithKey returns null for garbage ciphertext`() {
        val (_, privateKey) = CryptoService.generateKeyPair()

        val result = CryptoService.decryptWithKey("not-valid-base64-ciphertext!!!", privateKey)
        assertNull(result)
    }

    @Test
    fun `decryptWithKey returns null for garbage private key`() {
        CryptoService.initialize()
        val publicKey = CryptoService.getPublicKey()
        val ciphertext = CryptoService.encrypt("hello world", publicKey)
        assertNotNull(ciphertext)

        val result = CryptoService.decryptWithKey(ciphertext, "not-a-valid-private-key")
        assertNull(result)
    }

    @Test
    fun `encrypt returns null for an invalid public key`() {
        val result = CryptoService.encrypt("hello world", "not-a-valid-public-key")
        assertNull(result)
    }

    @Test
    fun `decrypt round-trips through the shared singleton key pair`() {
        CryptoService.initialize()
        val publicKey = CryptoService.getPublicKey()

        val ciphertext = CryptoService.encrypt("round trip me", publicKey)
        assertNotNull(ciphertext)

        val plaintext = CryptoService.decrypt(ciphertext)
        assertEquals("round trip me", plaintext)
    }

    @Test
    fun `decrypt returns null for garbage ciphertext`() {
        CryptoService.initialize()
        val result = CryptoService.decrypt("not-valid-base64-ciphertext!!!")
        assertNull(result)
    }
}
