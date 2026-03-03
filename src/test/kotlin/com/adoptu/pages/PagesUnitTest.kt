package com.adoptu.pages

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.html
import kotlinx.html.stream.createHTML

class PagesUnitTest : DescribeSpec({

    describe("IndexPage") {
        it("renders with required elements") {
            val html = createHTML().html {
                indexPage()
            }
            html.shouldContain("Adopt-U")
            html.shouldContain("Browse Pets")
            html.shouldContain("hero")
            html.shouldContain("logo")
            html.shouldContain("lang-dropdown")
        }

        it("renders navigation links") {
            val html = createHTML().html {
                indexPage()
            }
            html.shouldContain("href=\"/pets\"")
            html.shouldContain("href=\"/login\"")
            html.shouldContain("href=\"/register\"")
            html.shouldContain("href=\"/my-pets\"")
            html.shouldContain("href=\"/admin\"")
        }

        it("renders hero section with text") {
            val html = createHTML().html {
                indexPage()
            }
            html.shouldContain("Find Your New Best Friend")
            html.shouldContain("Dogs, cats, birds, fish")
        }

        it("includes required scripts") {
            val html = createHTML().html {
                indexPage()
            }
            html.shouldContain("/static/js/api.js")
            html.shouldContain("/static/js/i18n.js")
        }
    }

    describe("PetsPage") {
        it("renders with required elements") {
            val html = createHTML().html {
                petsPage()
            }
            html.shouldContain("Pets for Adoption")
            html.shouldContain("pet-grid")
            html.shouldContain("filter-buttons")
        }

        it("renders filter buttons for all pet types") {
            val html = createHTML().html {
                petsPage()
            }
            html.shouldContain("data-type=\"\"")
            html.shouldContain("data-type=\"DOG\"")
            html.shouldContain("data-type=\"CAT\"")
            html.shouldContain("data-type=\"BIRD\"")
            html.shouldContain("data-type=\"FISH\"")
        }

        it("renders sex filter") {
            val html = createHTML().html {
                petsPage()
            }
            html.shouldContain("filter-sex")
            html.shouldContain("MALE")
            html.shouldContain("FEMALE")
        }

        it("renders navigation links") {
            val html = createHTML().html {
                petsPage()
            }
            html.shouldContain("href=\"/\"")
            html.shouldContain("href=\"/pets\"")
            html.shouldContain("href=\"/login\"")
            html.shouldContain("href=\"/register\"")
        }
    }

    describe("Shared functions") {
        it("commonHead includes title and meta tags") {
            val html = createHTML().html {
                commonHead("Test Title")
            }
            html.shouldContain("Test Title")
            html.shouldContain("charset=\"UTF-8\"")
            html.shouldContain("viewport")
            html.shouldContain("/static/css/style.css")
        }

        it("languageDropdown renders language options") {
            val html = createHTML().html {
                body {
                    div(classes = "test-container") {
                        languageDropdown()
                    }
                }
            }
            html.shouldContain("lang-dropdown")
            html.shouldContain("lang-dropbtn")
            html.shouldContain("lang-option")
            html.shouldContain("data-lang=\"en\"")
            html.shouldContain("data-lang=\"es\"")
        }
    }

    describe("LoginPage") {
        it("renders with required elements") {
            val html = createHTML().html {
                loginPage()
            }
            html.shouldContain("Login with Passkey")
            html.shouldContain("login-btn")
            html.shouldContain("auth-form")
            html.shouldContain("message")
        }

        it("includes webauthn script") {
            val html = createHTML().html {
                loginPage()
            }
            html.shouldContain("/static/js/webauthn.js")
        }

        it("renders navigation links") {
            val html = createHTML().html {
                loginPage()
            }
            html.shouldContain("href=\"/\"")
            html.shouldContain("href=\"/pets\"")
            html.shouldContain("href=\"/register\"")
        }
    }

    describe("RegisterPage") {
        it("renders with required elements") {
            val html = createHTML().html {
                registerPage()
            }
            html.shouldContain("Create Account")
            html.shouldContain("register-form")
            html.shouldContain("username")
            html.shouldContain("displayName")
            html.shouldContain("role")
        }

        it("renders role options") {
            val html = createHTML().html {
                registerPage()
            }
            html.shouldContain("value=\"ADOPTER\"")
            html.shouldContain("value=\"RESCUER\"")
        }

        it("includes webauthn script") {
            val html = createHTML().html {
                registerPage()
            }
            html.shouldContain("/static/js/webauthn.js")
        }

        it("renders login link") {
            val html = createHTML().html {
                registerPage()
            }
            html.shouldContain("href=\"/login\"")
            html.shouldContain("Already have an account")
        }
    }

    describe("MyPetsPage") {
        it("renders with required elements") {
            val html = createHTML().html {
                myPetsPage()
            }
            html.shouldContain("My Pets")
            html.shouldContain("pet-form")
            html.shouldContain("add-btn")
            html.shouldContain("form-container")
        }

        it("renders pet form fields") {
            val html = createHTML().html {
                myPetsPage()
            }
            html.shouldContain("id=\"name\"")
            html.shouldContain("id=\"type\"")
            html.shouldContain("id=\"breed\"")
            html.shouldContain("id=\"description\"")
            html.shouldContain("id=\"sex\"")
        }

        it("renders pet type options") {
            val html = createHTML().html {
                myPetsPage()
            }
            html.shouldContain("value=\"DOG\"")
            html.shouldContain("value=\"CAT\"")
            html.shouldContain("value=\"BIRD\"")
            html.shouldContain("value=\"FISH\"")
        }

        it("renders size options") {
            val html = createHTML().html {
                myPetsPage()
            }
            html.shouldContain("value=\"SMALL\"")
            html.shouldContain("value=\"MEDIUM\"")
            html.shouldContain("value=\"LARGE\"")
        }

        it("renders sex options") {
            val html = createHTML().html {
                myPetsPage()
            }
            html.shouldContain("value=\"MALE\"")
            html.shouldContain("value=\"FEMALE\"")
        }

        it("renders checkbox groups for medical and compatibility") {
            val html = createHTML().html {
                myPetsPage()
            }
            html.shouldContain("checkbox-group")
            html.shouldContain("isSterilized")
            html.shouldContain("isMicrochipped")
            html.shouldContain("isGoodWithKids")
            html.shouldContain("isGoodWithDogs")
            html.shouldContain("isGoodWithCats")
            html.shouldContain("isHouseTrained")
        }
    }

    describe("PetDetailPage") {
        it("renders with required elements") {
            val html = createHTML().html {
                petDetailPage()
            }
            html.shouldContain("pet-detail")
            html.shouldContain("message")
        }

        it("renders navigation links") {
            val html = createHTML().html {
                petDetailPage()
            }
            html.shouldContain("href=\"/\"")
            html.shouldContain("href=\"/pets\"")
            html.shouldContain("href=\"/login\"")
            html.shouldContain("href=\"/register\"")
            html.shouldContain("href=\"/my-pets\"")
            html.shouldContain("href=\"/admin\"")
        }

        it("includes common scripts") {
            val html = createHTML().html {
                petDetailPage()
            }
            html.shouldContain("/static/js/api.js")
            html.shouldContain("/static/js/i18n.js")
        }
    }

    describe("AdminPage") {
        it("renders with required elements") {
            val html = createHTML().html {
                adminPage()
            }
            html.shouldContain("Admin Panel")
            html.shouldContain("Manage all pet pages")
            html.shouldContain("pet-grid")
            html.shouldContain("message")
        }

        it("renders navigation links") {
            val html = createHTML().html {
                adminPage()
            }
            html.shouldContain("href=\"/\"")
            html.shouldContain("href=\"/pets\"")
            html.shouldContain("href=\"/my-pets\"")
            html.shouldContain("href=\"/admin\"")
        }

        it("renders manage pets button") {
            val html = createHTML().html {
                adminPage()
            }
            html.shouldContain("Manage Pets")
            html.shouldContain("href=\"/my-pets\"")
        }
    }
})
