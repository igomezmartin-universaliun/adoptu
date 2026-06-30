package com.adoptu.services.validation

import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.dto.input.UserDto
import com.adoptu.dto.input.UserRole
import com.adoptu.mocks.TestClock
import com.adoptu.mocks.TestDatabase
import com.adoptu.services.ServiceResult
import com.adoptu.services.UserService
import com.adoptu.services.auth.SessionUser
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class PetsValidationServiceTest {

    private lateinit var service: PetsValidationService
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

        service = PetsValidationService()
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

    private fun userDto(roles: Set<UserRole>): UserDto = UserDto(
        id = 1,
        username = "user@test.com",
        displayName = "Test User",
        activeRoles = roles
    )

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

    // validateUser

    @Test
    fun `validateUser returns Success when user is not null`() {
        val user = userDto(emptySet())

        val result = service.validateUser(user)

        assertIs<ServiceResult.Success<UserDto>>(result)
        assertEquals(user, result.data)
    }

    @Test
    fun `validateUser returns NotFound when user is null`() {
        val result = service.validateUser(null)

        assertIs<ServiceResult.NotFound>(result)
    }

    // validateId

    @Test
    fun `validateId returns Success for valid numeric id`() {
        val result = service.validateId("5")

        assertIs<ServiceResult.Success<Int>>(result)
        assertEquals(5, result.data)
    }

    @Test
    fun `validateId returns Error for null id`() {
        val result = service.validateId(null)

        assertIs<ServiceResult.Error<Int>>(result)
        assertEquals(ValidationConstants.INVALID_ID, result.message)
    }

    @Test
    fun `validateId returns Error for non-numeric id`() {
        val result = service.validateId("xyz")

        assertIs<ServiceResult.Error<Int>>(result)
        assertEquals(ValidationConstants.INVALID_ID, result.message)
    }

    // validateRequired

    @Test
    fun `validateRequired returns Success for non-blank value`() {
        val result = service.validateRequired("Rex", "Name")

        assertIs<ServiceResult.Success<String>>(result)
        assertEquals("Rex", result.data)
    }

    @Test
    fun `validateRequired returns Error for null value`() {
        val result = service.validateRequired(null, "Name")

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("Name is required", result.message)
    }

    @Test
    fun `validateRequired returns Error for blank value`() {
        val result = service.validateRequired("  ", "Breed")

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("Breed is required", result.message)
    }

    // validateRole

    @Test
    fun `validateRole returns Success when user has required role`() {
        val user = userDto(setOf(UserRole.RESCUER))

        val result = service.validateRole(user, "RESCUER")

        assertIs<ServiceResult.Success<Unit>>(result)
    }

    @Test
    fun `validateRole returns Success when user is admin`() {
        val user = userDto(setOf(UserRole.ADMIN))

        val result = service.validateRole(user, "RESCUER")

        assertIs<ServiceResult.Success<Unit>>(result)
    }

    @Test
    fun `validateRole returns Forbidden when user lacks required role`() {
        val user = userDto(setOf(UserRole.ADOPTER))

        val result = service.validateRole(user, "RESCUER")

        assertIs<ServiceResult.Forbidden>(result)
    }

    // validateRoles

    @Test
    fun `validateRoles returns Success when user has one of the required roles`() {
        val user = userDto(setOf(UserRole.PHOTOGRAPHER))

        val result = service.validateRoles(user, listOf("RESCUER", "PHOTOGRAPHER"))

        assertIs<ServiceResult.Success<Unit>>(result)
    }

    @Test
    fun `validateRoles returns Success when user is admin`() {
        val user = userDto(setOf(UserRole.ADMIN))

        val result = service.validateRoles(user, listOf("RESCUER", "PHOTOGRAPHER"))

        assertIs<ServiceResult.Success<Unit>>(result)
    }

    @Test
    fun `validateRoles returns Forbidden when user has none of the required roles`() {
        val user = userDto(setOf(UserRole.ADOPTER))

        val result = service.validateRoles(user, listOf("RESCUER", "PHOTOGRAPHER"))

        assertIs<ServiceResult.Forbidden>(result)
    }

    // validateStatus

    @Test
    fun `validateStatus returns Success for valid status`() {
        val result = service.validateStatus("AVAILABLE", listOf("AVAILABLE", "ADOPTED"))

        assertIs<ServiceResult.Success<String>>(result)
        assertEquals("AVAILABLE", result.data)
    }

    @Test
    fun `validateStatus returns Error for null status`() {
        val result = service.validateStatus(null, listOf("AVAILABLE", "ADOPTED"))

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("Invalid status", result.message)
    }

    @Test
    fun `validateStatus returns Error for status not in valid list`() {
        val result = service.validateStatus("UNKNOWN", listOf("AVAILABLE", "ADOPTED"))

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("Invalid status", result.message)
    }
}
