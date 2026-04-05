package com.adoptu.e2e

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Flaky E2E tests due to Playwright browser infrastructure issues")
class FrontendE2ETest : BaseE2ETest() {

    @Test
    fun `index page loads without JavaScript errors`() {
        navigateTo("/")
        assertNoErrors()
    }

    @Test
    fun `login page loads without JavaScript errors`() {
        navigateTo("/login")
        assertNoErrors()
    }

    @Test
    fun `register page loads without JavaScript errors`() {
        navigateTo("/register")
        assertNoErrors()
    }

    @Test
    fun `pets page loads without JavaScript errors`() {
        navigateTo("/pets")
        assertNoErrors()
    }

    @Test
    fun `photographers page loads without JavaScript errors`() {
        navigateTo("/photographers")
        assertNoErrors()
    }

    @Test
    fun `temporal homes page loads without JavaScript errors`() {
        navigateTo("/temporal-homes")
        assertNoErrors()
    }

    @Test
    @Disabled("Flaky test - fails when run with all tests due to port/resource contention")
    fun `shelters page loads without JavaScript errors`() {
        navigateTo("/shelters")
        assertNoErrors()
    }

    @Test
    fun `profile page loads without JavaScript errors`() {
        navigateTo("/profile")
        assertNoErrors()
    }

    @Test
    fun `my pets page loads without JavaScript errors`() {
        navigateTo("/my-pets")
        assertNoErrors()
    }

    @Test
    fun `privacy page loads without JavaScript errors`() {
        navigateTo("/privacy")
        assertNoErrors()
    }

    @Test
    fun `terms page loads without JavaScript errors`() {
        navigateTo("/terms")
        assertNoErrors()
    }
}
