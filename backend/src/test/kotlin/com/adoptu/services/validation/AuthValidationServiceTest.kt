package com.adoptu.services.validation

import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.dto.input.UserDto
import com.adoptu.mocks.TestClock
import com.adoptu.mocks.TestDatabase
import com.adoptu.services.ServiceResult
import com.adoptu.services.UserService
import com.adoptu.services.auth.SessionUser
import com.adoptu.services.crypto.CryptoService
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class AuthValidationServiceTest {

    private lateinit var service: AuthValidationService
    private lateinit var userService: UserService
    private val clock: TestClock = TestClock(Instant.parse("2024-01-15T10:00:00Z"))

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        val userRepository = UserRepository(clock)
        userService = UserService(userRepository)

        stopKoin() // defensive: clear any Koin app leaked from a concurrently-run test in this JVM
        startKoin {
            modules(module {
                single { userService }
            })
        }

        service = AuthValidationService()
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun createTestUser(
        username: String = "user@test.com",
        displayName: String = "Test User",
        role: String = "ADOPTER"
    ): Int {
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = displayName
                it[Users.createdAt] = clock.now().toEpochMilliseconds()
            } get Users.id
        }
        transaction {
            UserActiveRoles.insert {
                it[UserActiveRoles.userId] = userId
                it[UserActiveRoles.role] = role
            }
        }
        return userId
    }

    private fun encrypted(plaintext: String): String {
        val publicKey = CryptoService.getPublicKey()
        return CryptoService.encrypt(plaintext, publicKey)!!
    }

    // validateAndDecryptEmail

    @Test
    fun `validateAndDecryptEmail returns Success for valid encrypted email`() {
        val data = encrypted("user@test.com")

        val result = service.validateAndDecryptEmail(data)

        assertIs<ServiceResult.Success<String>>(result)
        assertEquals("user@test.com", result.data)
    }

    @Test
    fun `validateAndDecryptEmail returns Error when decryption fails`() {
        val result = service.validateAndDecryptEmail("not-a-valid-ciphertext!!")

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("Failed to decrypt email", result.message)
    }

    @Test
    fun `validateAndDecryptEmail returns Error for invalid email format`() {
        val data = encrypted("not-an-email")

        val result = service.validateAndDecryptEmail(data)

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("Invalid email format", result.message)
    }

    // validateEmailAndUser

    @Test
    fun `validateEmailAndUser returns Error for invalid email format`() {
        val result = service.validateEmailAndUser("not-an-email")

        assertIs<ServiceResult.Error<UserDto>>(result)
        assertEquals("Invalid email format", result.message)
    }

    @Test
    fun `validateEmailAndUser returns Error when user not found`() {
        val result = service.validateEmailAndUser("missing@test.com")

        assertIs<ServiceResult.Error<UserDto>>(result)
        assertEquals("Invalid credentials", result.message)
    }

    @Test
    fun `validateEmailAndUser returns Success when user found`() {
        createTestUser(username = "found@test.com")

        val result = service.validateEmailAndUser("found@test.com")

        assertIs<ServiceResult.Success<UserDto>>(result)
        assertEquals("found@test.com", result.data.username)
    }

    // validateSession

    @Test
    fun `validateSession returns Forbidden when session is null`() {
        val result = service.validateSession(null)

        assertIs<ServiceResult.Forbidden>(result)
    }

    @Test
    fun `validateSession returns Success when session is present`() {
        val session = SessionUser(userId = 1, email = "a@test.com", displayName = "A")

        val result = service.validateSession(session)

        assertIs<ServiceResult.Success<SessionUser>>(result)
        assertEquals(session, result.data)
    }

    // validateUserById

    @Test
    fun `validateUserById returns Success when user exists`() {
        val userId = createTestUser()

        val result = service.validateUserById(userId)

        assertIs<ServiceResult.Success<UserDto>>(result)
        assertEquals(userId, result.data.id)
    }

    @Test
    fun `validateUserById returns NotFound when user does not exist`() {
        val result = service.validateUserById(999)

        assertIs<ServiceResult.NotFound>(result)
    }

    // validateNotBanned

    @Test
    fun `validateNotBanned returns Success when user is not banned`() {
        val userId = createTestUser()

        val result = service.validateNotBanned(userId)

        assertIs<ServiceResult.Success<UserDto>>(result)
        assertEquals(userId, result.data.id)
    }

    @Test
    fun `validateNotBanned returns NotFound when user does not exist`() {
        val result = service.validateNotBanned(999)

        assertIs<ServiceResult.NotFound>(result)
    }

    @Test
    fun `validateNotBanned returns Error with reason when user is banned with a reason`() {
        val userId = createTestUser()
        userService.banUser(userId, "Spam activity")

        val result = service.validateNotBanned(userId)

        assertIs<ServiceResult.Error<UserDto>>(result)
        assertEquals("Your account has been suspended. Reason: Spam activity", result.message)
    }

    @Test
    fun `validateNotBanned returns Error with default reason when user is banned without a reason`() {
        val userId = createTestUser()
        userService.banUser(userId)

        val result = service.validateNotBanned(userId)

        assertIs<ServiceResult.Error<UserDto>>(result)
        assertEquals("Your account has been suspended. Reason: Contact administrator", result.message)
    }

    // validateVerified

    @Test
    fun `validateVerified returns Error with email when user is not verified`() {
        val userId = createTestUser()

        val result = service.validateVerified(userId, "user@test.com")

        assertIs<ServiceResult.Error<Unit>>(result)
        assertEquals("user@test.com", result.message)
    }

    @Test
    fun `validateVerified returns Success when user is verified`() {
        val userId = createTestUser()
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.isEmailVerified] = true
            }
        }

        val result = service.validateVerified(userId, "user@test.com")

        assertIs<ServiceResult.Success<Unit>>(result)
    }

    // getUserByEmail

    @Test
    fun `getUserByEmail returns user when found`() {
        createTestUser(username = "lookup@test.com")

        val result = service.getUserByEmail("lookup@test.com")

        assertEquals("lookup@test.com", result?.username)
    }

    @Test
    fun `getUserByEmail returns null when not found`() {
        val result = service.getUserByEmail("missing@test.com")

        assertNull(result)
    }
}
