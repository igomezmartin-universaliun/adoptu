package com.adoptu.pages

import com.adoptu.pages.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class PagesTest {

    @Test
    fun `IndexPage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.IndexPageKt")
        assertNotNull(page)
    }

    @Test
    fun `LoginPage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.LoginPageKt")
        assertNotNull(page)
    }

    @Test
    fun `RegisterPage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.RegisterPageKt")
        assertNotNull(page)
    }

    @Test
    fun `ProfilePage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.ProfilePageKt")
        assertNotNull(page)
    }

    @Test
    fun `PetsPage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.PetsPageKt")
        assertNotNull(page)
    }

    @Test
    fun `TermsPage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.TermsPageKt")
        assertNotNull(page)
    }

    @Test
    fun `PrivacyPage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.PrivacyPageKt")
        assertNotNull(page)
    }

    @Test
    fun `PhotographersPage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.PhotographersPageKt")
        assertNotNull(page)
    }

    @Test
    fun `TemporalHomePage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.TemporalHomePageKt")
        assertNotNull(page)
    }

    @Test
    fun `PetDetailPage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.PetDetailPageKt")
        assertNotNull(page)
    }

    @Test
    fun `MyPetsPage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.MyPetsPageKt")
        assertNotNull(page)
    }

    @Test
    fun `AdminPage exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.AdminPageKt")
        assertNotNull(page)
    }

    @Test
    fun `Shared module exists`() {
        val page = PagesTest::class.java.classLoader.loadClass("com.adoptu.pages.SharedKt")
        assertNotNull(page)
    }

    @Test
    fun `all page classes can be loaded`() {
        val pageClasses = listOf(
            "com.adoptu.pages.IndexPageKt",
            "com.adoptu.pages.LoginPageKt",
            "com.adoptu.pages.RegisterPageKt",
            "com.adoptu.pages.ProfilePageKt",
            "com.adoptu.pages.PetsPageKt",
            "com.adoptu.pages.TermsPageKt",
            "com.adoptu.pages.PrivacyPageKt",
            "com.adoptu.pages.PhotographersPageKt",
            "com.adoptu.pages.TemporalHomePageKt",
            "com.adoptu.pages.PetDetailPageKt",
            "com.adoptu.pages.MyPetsPageKt",
            "com.adoptu.pages.AdminPageKt",
            "com.adoptu.pages.SharedKt"
        )
        pageClasses.forEach { className ->
            val clazz = PagesTest::class.java.classLoader.loadClass(className)
            assertNotNull(clazz, "Should be able to load $className")
        }
    }

    @Test
    fun `page files exist in source`() {
        val basePath = "src/main/kotlin/com/adoptu/pages"
        listOf(
            "$basePath/IndexPage.kt",
            "$basePath/LoginPage.kt",
            "$basePath/RegisterPage.kt",
            "$basePath/ProfilePage.kt",
            "$basePath/PetsPage.kt",
            "$basePath/TermsPage.kt",
            "$basePath/PrivacyPage.kt",
            "$basePath/PhotographersPage.kt",
            "$basePath/TemporalHomePage.kt",
            "$basePath/PetDetailPage.kt",
            "$basePath/MyPetsPage.kt",
            "$basePath/AdminPage.kt",
            "$basePath/Shared.kt"
        ).forEach { path ->
            val file = java.io.File(path)
            assertTrue(file.exists(), "Page file should exist: $path")
        }
    }

    @Test
    fun `page source files contain expected function definitions`() {
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/IndexPage.kt").readText().contains("fun HTML.indexPage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/LoginPage.kt").readText().contains("fun HTML.loginPage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/RegisterPage.kt").readText().contains("fun HTML.registerPage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/ProfilePage.kt").readText().contains("fun HTML.profilePage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/PetsPage.kt").readText().contains("fun HTML.petsPage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/TermsPage.kt").readText().contains("fun HTML.termsPage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/PrivacyPage.kt").readText().contains("fun HTML.privacyPage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/PhotographersPage.kt").readText().contains("fun HTML.photographersPage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/TemporalHomePage.kt").readText().contains("fun HTML.temporalHomeProfilePage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/TemporalHomePage.kt").readText().contains("fun HTML.temporalHomesSearchPage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/PetDetailPage.kt").readText().contains("fun HTML.petDetailPage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/MyPetsPage.kt").readText().contains("fun HTML.myPetsPage()"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/AdminPage.kt").readText().contains("fun HTML.adminPage()"))
    }

    @Test
    fun `page source files contain expected content`() {
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/IndexPage.kt").readText().contains("Pets for Adoption"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/LoginPage.kt").readText().contains("Passkey"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/RegisterPage.kt").readText().contains("Create Account"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/ProfilePage.kt").readText().contains("Profile"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/TermsPage.kt").readText().contains("Terms and Conditions"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/PrivacyPage.kt").readText().contains("Privacy Policy"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/PhotographersPage.kt").readText().contains("Photographers"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/TemporalHomePage.kt").readText().contains("Temporal Home"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/PetDetailPage.kt").readText().contains("Pet Details"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/MyPetsPage.kt").readText().contains("My Pets"))
        assertTrue(java.io.File("src/main/kotlin/com/adoptu/pages/AdminPage.kt").readText().contains("Admin"))
    }

    @Test
    fun `Shared module contains common functions`() {
        val sharedContent = java.io.File("src/main/kotlin/com/adoptu/pages/Shared.kt").readText()
        assertTrue(sharedContent.contains("fun HTML.commonHead"))
        assertTrue(sharedContent.contains("fun NAV.commonNav"))
        assertTrue(sharedContent.contains("fun NAV.guestNav"))
        assertTrue(sharedContent.contains("fun SELECT.countrySelect"))
        assertTrue(sharedContent.contains("fun BODY.footer"))
    }

    @Test
    fun `all pages include common scripts`() {
        listOf(
            "src/main/kotlin/com/adoptu/pages/LoginPage.kt",
            "src/main/kotlin/com/adoptu/pages/RegisterPage.kt",
            "src/main/kotlin/com/adoptu/pages/ProfilePage.kt",
            "src/main/kotlin/com/adoptu/pages/PetsPage.kt"
        ).forEach { path ->
            val content = java.io.File(path).readText()
            assertTrue(content.contains("commonScripts()") || content.contains("api.js") || content.contains("i18n.js"), "Page should include common scripts: $path")
        }
    }

    @Test
    fun `all pages include footer`() {
        listOf(
            "src/main/kotlin/com/adoptu/pages/IndexPage.kt",
            "src/main/kotlin/com/adoptu/pages/LoginPage.kt",
            "src/main/kotlin/com/adoptu/pages/TermsPage.kt",
            "src/main/kotlin/com/adoptu/pages/PrivacyPage.kt"
        ).forEach { path ->
            val content = java.io.File(path).readText()
            assertTrue(content.contains("footer()"), "Page should include footer: $path")
        }
    }

    @Test
    fun `Guest pages use guestNav`() {
        listOf(
            "src/main/kotlin/com/adoptu/pages/LoginPage.kt",
            "src/main/kotlin/com/adoptu/pages/RegisterPage.kt",
            "src/main/kotlin/com/adoptu/pages/TermsPage.kt",
            "src/main/kotlin/com/adoptu/pages/PrivacyPage.kt"
        ).forEach { path ->
            val content = java.io.File(path).readText()
            assertTrue(content.contains("guestNav()"), "Guest page should use guestNav: $path")
        }
    }

    @Test
    fun `Authenticated pages use commonNav`() {
        listOf(
            "src/main/kotlin/com/adoptu/pages/ProfilePage.kt",
            "src/main/kotlin/com/adoptu/pages/PetsPage.kt",
            "src/main/kotlin/com/adoptu/pages/PhotographersPage.kt",
            "src/main/kotlin/com/adoptu/pages/MyPetsPage.kt"
        ).forEach { path ->
            val content = java.io.File(path).readText()
            assertTrue(content.contains("commonNav()"), "Authenticated page should use commonNav: $path")
        }
    }
}
