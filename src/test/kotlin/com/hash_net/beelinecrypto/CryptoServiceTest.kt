package com.hash_net.beelinecrypto

import com.adoptu.services.crypto.CryptoService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class CryptoServiceTest {

    @BeforeEach
    fun setup() {
        CryptoService.initialize()
    }

    @Test
    fun `initialize creates key pair`() {
        val publicKey = CryptoService.getPublicKey()
        assertNotNull(publicKey)
        assertTrue(publicKey.isNotEmpty())
        assertTrue(publicKey.startsWith("MIIBIj"))
    }

    @Test
    fun `getPublicKey returns consistent key`() {
        val key1 = CryptoService.getPublicKey()
        val key2 = CryptoService.getPublicKey()
        assertEquals(key1, key2)
    }

    @Test
    fun `encrypt and decrypt works with valid public key`() {
        val plaintext = "Hello, World!"
        val publicKey = CryptoService.getPublicKey()

        val encrypted = CryptoService.encrypt(plaintext, publicKey)
        assertNotNull(encrypted)
        assertNotEquals(plaintext, encrypted)

        val decrypted = CryptoService.decrypt(encrypted)
        assertNotNull(decrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt produces different ciphertexts for same plaintext`() {
        val plaintext = "Hello, World!"
        val publicKey = CryptoService.getPublicKey()

        val encrypted1 = CryptoService.encrypt(plaintext, publicKey)
        val encrypted2 = CryptoService.encrypt(plaintext, publicKey)

        assertNotNull(encrypted1)
        assertNotNull(encrypted2)
        assertNotEquals(encrypted1, encrypted2)
    }

    @Test
    fun `encrypt returns null for invalid public key`() {
        val plaintext = "Hello, World!"
        val invalidKey = "invalid-base64-key"

        val result = CryptoService.encrypt(plaintext, invalidKey)
        assertNull(result)
    }

    @Test
    fun `decrypt returns null for invalid ciphertext`() {
        val result = CryptoService.decrypt("invalid-ciphertext")
        assertNull(result)
    }

    @Test
    fun `decrypt returns null for tampered ciphertext`() {
        val plaintext = "Secret password123"
        val publicKey = CryptoService.getPublicKey()

        val encrypted = CryptoService.encrypt(plaintext, publicKey)
        assertNotNull(encrypted)

        val tampered = encrypted.dropLast(5) + "XXXXX"
        val result = CryptoService.decrypt(tampered)
        assertNull(result)
    }

    @Test
    fun `encrypt and decrypt works with long text`() {
        val plaintext = "a".repeat(100)
        val publicKey = CryptoService.getPublicKey()

        val encrypted = CryptoService.encrypt(plaintext, publicKey)
        assertNotNull(encrypted)

        val decrypted = CryptoService.decrypt(encrypted)
        assertNotNull(decrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt and decrypt works with special characters`() {
        val plaintext = "Password!@#\$%^&*()_+-=[]{}|;':\",./<>?日本語中文한국어"
        val publicKey = CryptoService.getPublicKey()

        val encrypted = CryptoService.encrypt(plaintext, publicKey)
        assertNotNull(encrypted)

        val decrypted = CryptoService.decrypt(encrypted)
        assertNotNull(decrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt and decrypt works with empty string`() {
        val plaintext = ""
        val publicKey = CryptoService.getPublicKey()

        val encrypted = CryptoService.encrypt(plaintext, publicKey)
        assertNotNull(encrypted)

        val decrypted = CryptoService.decrypt(encrypted)
        assertNotNull(decrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt and decrypt works with unicode characters`() {
        val plaintext = "密码测试 пароль 테스트 𝕋𝕖𝕤𝕥𝕚𝕟𝕘🔐"
        val publicKey = CryptoService.getPublicKey()

        val encrypted = CryptoService.encrypt(plaintext, publicKey)
        assertNotNull(encrypted)

        val decrypted = CryptoService.decrypt(encrypted)
        assertNotNull(decrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `generateKeyPair returns valid key pair`() {
        val (publicKey, privateKey) = CryptoService.generateKeyPair()

        assertNotNull(publicKey)
        assertNotNull(privateKey)
        assertTrue(publicKey.isNotEmpty(), "Public key is empty")
        assertTrue(privateKey.isNotEmpty(), "Private key is empty")
        assertTrue(publicKey.startsWith("MIIBIj"), "Public key should be X509 format starting with MIIBIj")
        assertTrue(privateKey.startsWith("MIIE"), "Private key should be PKCS8 format starting with MIIE")
    }

    @Test
    fun `encrypt with one key pair can be decrypted`() {
        val (publicKey, _) = CryptoService.generateKeyPair()
        val plaintext = "Cross-key encryption test"

        val encrypted = CryptoService.encrypt(plaintext, publicKey)
        assertNotNull(encrypted)

        val decrypted = CryptoService.decrypt(encrypted)
        assertNotNull(decrypted)
        assertEquals(plaintext, decrypted)
    }
}
