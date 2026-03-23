package com.adoptu.services

import com.adoptu.dto.*
import com.adoptu.ports.NotificationPort
import com.adoptu.ports.TemporalHomeRepositoryPort
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemporalHomeServiceTest {

    @MockK
    private lateinit var mockTemporalHomeRepository: TemporalHomeRepositoryPort

    @MockK
    private lateinit var mockNotificationAdapter: NotificationPort

    @MockK
    private lateinit var mockUserService: UserService

    @InjectMockKs
    private lateinit var temporalHomeService: TemporalHomeService

    private val testTemporalHome = TemporalHomeDto(
        userId = 1,
        alias = "Happy Paws",
        country = "USA",
        state = "CA",
        city = "Los Angeles",
        zip = "90001",
        neighborhood = "Downtown",
        createdAt = System.currentTimeMillis()
    )

    private val testRescuer = UserDto(
        id = 2,
        username = "rescuer@test.com",
        displayName = "Test Rescuer",
        activeRoles = setOf(UserRole.RESCUER)
    )

    private val testTemporalHomeUser = UserDto(
        id = 1,
        username = "temporalhome@test.com",
        displayName = "Test Temporal Home",
        activeRoles = setOf(UserRole.TEMPORAL_HOME)
    )

    private val testTemporalHomeRequest = TemporalHomeRequestDto(
        id = 1,
        temporalHomeId = 1,
        temporalHomeAlias = "Happy Paws",
        rescuerId = 2,
        rescuerName = "Test Rescuer",
        petId = null,
        petName = null,
        message = "Can I send my pet?",
        status = "SENT",
        createdAt = System.currentTimeMillis()
    )

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getTemporalHome returns temporal home from repository`() {
        every { mockTemporalHomeRepository.getTemporalHome(1) } returns testTemporalHome

        val result = temporalHomeService.getTemporalHome(1)

        assertNotNull(result)
        assertEquals(1, result.userId)
        assertEquals("Happy Paws", result.alias)
        verify { mockTemporalHomeRepository.getTemporalHome(1) }
    }

    @Test
    fun `getTemporalHome returns null when not found`() {
        every { mockTemporalHomeRepository.getTemporalHome(999) } returns null

        val result = temporalHomeService.getTemporalHome(999)

        assertNull(result)
    }

    @Test
    fun `createTemporalHome calls repository with correct params`() {
        val request = CreateTemporalHomeRequest(
            alias = "New Home",
            country = "USA",
            city = "San Francisco"
        )
        every { mockTemporalHomeRepository.createTemporalHome(1, request) } returns testTemporalHome.copy(alias = "New Home")

        val result = temporalHomeService.createTemporalHome(1, request)

        assertNotNull(result)
        assertEquals("New Home", result.alias)
        verify { mockTemporalHomeRepository.createTemporalHome(1, request) }
    }

    @Test
    fun `updateTemporalHome calls repository with correct params`() {
        val request = UpdateTemporalHomeRequest(alias = "Updated Home")
        every { mockTemporalHomeRepository.updateTemporalHome(1, request) } returns testTemporalHome.copy(alias = "Updated Home")

        val result = temporalHomeService.updateTemporalHome(1, request)

        assertNotNull(result)
        assertEquals("Updated Home", result.alias)
        verify { mockTemporalHomeRepository.updateTemporalHome(1, request) }
    }

    @Test
    fun `updateTemporalHome returns null when not found`() {
        val request = UpdateTemporalHomeRequest(alias = "Updated Home")
        every { mockTemporalHomeRepository.updateTemporalHome(999, request) } returns null

        val result = temporalHomeService.updateTemporalHome(999, request)

        assertNull(result)
    }

    @Test
    fun `searchTemporalHomes returns results from repository`() {
        val params = TemporalHomeSearchParams(country = "USA", state = "CA")
        every { mockTemporalHomeRepository.searchTemporalHomes(params) } returns listOf(testTemporalHome)

        val result = temporalHomeService.searchTemporalHomes(params)

        assertEquals(1, result.size)
        assertEquals("Happy Paws", result.first().alias)
        verify { mockTemporalHomeRepository.searchTemporalHomes(params) }
    }

    @Test
    fun `searchTemporalHomes returns empty list when no matches`() {
        val params = TemporalHomeSearchParams(country = "France")
        every { mockTemporalHomeRepository.searchTemporalHomes(params) } returns emptyList()

        val result = temporalHomeService.searchTemporalHomes(params)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `sendRequest succeeds when rescuer sends request`() {
        val request = SendTemporalHomeRequestRequest(
            temporalHomeId = 1,
            petId = null,
            message = "Can I send my pet?"
        )
        every { mockTemporalHomeRepository.getTemporalHome(1) } returns testTemporalHome
        every { mockTemporalHomeRepository.isBlocked(1, 2) } returns false
        every { mockUserService.getById(2) } returns testRescuer
        every { mockUserService.getById(1) } returns testTemporalHomeUser
        every { mockTemporalHomeRepository.createTemporalHomeRequest(1, 2, null, "Can I send my pet?") } returns 1

        val result = temporalHomeService.sendRequest(2, request)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        verify { mockTemporalHomeRepository.createTemporalHomeRequest(1, 2, null, "Can I send my pet?") }
    }

    @Test
    fun `sendRequest fails when temporal home not found`() {
        val request = SendTemporalHomeRequestRequest(
            temporalHomeId = 999,
            petId = null,
            message = "Test"
        )
        every { mockTemporalHomeRepository.getTemporalHome(999) } returns null

        val result = temporalHomeService.sendRequest(2, request)

        assertTrue(result.isFailure)
        assertEquals("Temporal home not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `sendRequest fails when rescuer is blocked`() {
        val request = SendTemporalHomeRequestRequest(
            temporalHomeId = 1,
            petId = null,
            message = "Test"
        )
        every { mockTemporalHomeRepository.getTemporalHome(1) } returns testTemporalHome
        every { mockTemporalHomeRepository.isBlocked(1, 2) } returns true

        val result = temporalHomeService.sendRequest(2, request)

        assertTrue(result.isFailure)
        assertEquals("You have been blocked by this temporal home", result.exceptionOrNull()?.message)
    }

    @Test
    fun `sendRequest fails when user not found`() {
        val request = SendTemporalHomeRequestRequest(
            temporalHomeId = 1,
            petId = null,
            message = "Test"
        )
        every { mockTemporalHomeRepository.getTemporalHome(1) } returns testTemporalHome
        every { mockTemporalHomeRepository.isBlocked(1, 999) } returns false
        every { mockUserService.getById(999) } returns null

        val result = temporalHomeService.sendRequest(999, request)

        assertTrue(result.isFailure)
        assertEquals("User not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `sendRequest fails when user is not rescuer or admin`() {
        val request = SendTemporalHomeRequestRequest(
            temporalHomeId = 1,
            petId = null,
            message = "Test"
        )
        val adopter = UserDto(
            id = 2,
            username = "adopter@test.com",
            displayName = "Test Adopter",
            activeRoles = setOf(UserRole.ADOPTER)
        )
        every { mockTemporalHomeRepository.getTemporalHome(1) } returns testTemporalHome
        every { mockTemporalHomeRepository.isBlocked(1, 2) } returns false
        every { mockUserService.getById(2) } returns adopter

        val result = temporalHomeService.sendRequest(2, request)

        assertTrue(result.isFailure)
        assertEquals("Only rescuers can send temporal home requests", result.exceptionOrNull()?.message)
    }

    @Test
    fun `sendRequest succeeds when admin sends request`() {
        val request = SendTemporalHomeRequestRequest(
            temporalHomeId = 1,
            petId = null,
            message = "Admin request"
        )
        val admin = UserDto(
            id = 2,
            username = "admin@test.com",
            displayName = "Admin User",
            activeRoles = setOf(UserRole.ADMIN)
        )
        every { mockTemporalHomeRepository.getTemporalHome(1) } returns testTemporalHome
        every { mockTemporalHomeRepository.isBlocked(1, 2) } returns false
        every { mockUserService.getById(2) } returns admin
        every { mockUserService.getById(1) } returns testTemporalHomeUser
        every { mockTemporalHomeRepository.createTemporalHomeRequest(1, 2, null, "Admin request") } returns 1
        coEvery { mockNotificationAdapter.sendTemporalHomeRequest(any(), any(), any(), any(), any(), any()) } returns true

        val result = temporalHomeService.sendRequest(2, request)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `sendRequest succeeds when rescuer has multiple roles including rescuer`() {
        val request = SendTemporalHomeRequestRequest(
            temporalHomeId = 1,
            petId = null,
            message = "Test"
        )
        val multiRoleUser = UserDto(
            id = 2,
            username = "rescuer@test.com",
            displayName = "Test Rescuer",
            activeRoles = setOf(UserRole.RESCUER, UserRole.ADOPTER)
        )
        every { mockTemporalHomeRepository.getTemporalHome(1) } returns testTemporalHome
        every { mockTemporalHomeRepository.isBlocked(1, 2) } returns false
        every { mockUserService.getById(2) } returns multiRoleUser
        every { mockUserService.getById(1) } returns testTemporalHomeUser
        every { mockTemporalHomeRepository.createTemporalHomeRequest(1, 2, null, "Test") } returns 1
        coEvery { mockNotificationAdapter.sendTemporalHomeRequest(any(), any(), any(), any(), any(), any()) } returns true

        val result = temporalHomeService.sendRequest(2, request)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `isBlocked returns value from repository`() {
        every { mockTemporalHomeRepository.isBlocked(1, 2) } returns true

        val result = temporalHomeService.isBlocked(1, 2)

        assertTrue(result)
        verify { mockTemporalHomeRepository.isBlocked(1, 2) }
    }

    @Test
    fun `isBlocked returns false when not blocked`() {
        every { mockTemporalHomeRepository.isBlocked(1, 2) } returns false

        val result = temporalHomeService.isBlocked(1, 2)

        assertFalse(result)
    }

    @Test
    fun `blockRescuer calls repository with correct params`() {
        every { mockTemporalHomeRepository.blockRescuer(1, 2) } returns true

        val result = temporalHomeService.blockRescuer(1, 2)

        assertTrue(result)
        verify { mockTemporalHomeRepository.blockRescuer(1, 2) }
    }

    @Test
    fun `blockRescuer returns false when fails`() {
        every { mockTemporalHomeRepository.blockRescuer(1, 2) } returns false

        val result = temporalHomeService.blockRescuer(1, 2)

        assertFalse(result)
    }

    @Test
    fun `getMyRequests returns requests from repository`() {
        every { mockTemporalHomeRepository.getMyRequests(2) } returns listOf(testTemporalHomeRequest)

        val result = temporalHomeService.getMyRequests(2)

        assertEquals(1, result.size)
        assertEquals("SENT", result.first().status)
        verify { mockTemporalHomeRepository.getMyRequests(2) }
    }

    @Test
    fun `getMyRequests returns empty list when no requests`() {
        every { mockTemporalHomeRepository.getMyRequests(2) } returns emptyList()

        val result = temporalHomeService.getMyRequests(2)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `sendRequest sends notification when email available`() {
        val request = SendTemporalHomeRequestRequest(
            temporalHomeId = 1,
            petId = null,
            message = "Test message"
        )
        every { mockTemporalHomeRepository.getTemporalHome(1) } returns testTemporalHome
        every { mockTemporalHomeRepository.isBlocked(1, 2) } returns false
        every { mockUserService.getById(2) } returns testRescuer
        every { mockUserService.getById(1) } returns testTemporalHomeUser
        every { mockTemporalHomeRepository.createTemporalHomeRequest(1, 2, null, "Test message") } returns 1
        coEvery { mockNotificationAdapter.sendTemporalHomeRequest(any(), any(), any(), any(), any(), any()) } returns true

        val result = temporalHomeService.sendRequest(2, request)

        assertTrue(result.isSuccess)
        coVerify { mockNotificationAdapter.sendTemporalHomeRequest(
            temporalHomeEmail = "temporalhome@test.com",
            temporalHomeAlias = "Happy Paws",
            rescuerName = "Test Rescuer",
            petName = null,
            message = "Test message",
            spamReportLink = any()
        ) }
    }
}

