# Adopt-U - Pet Adoption Platform

A Kotlin pet adoption web application with **FIDO2/WebAuthn** passwordless authentication. Multi-page application with HTML rendered from Kotlin (kotlinx.html).

## Features

- **FIDO2 Authentication**: Register and sign in with passkeys (biometrics, security keys)
- **Pet Types**: Dogs, cats, birds, fish
- **User Roles**:
  - **Admin**: Add or remove pet pages
  - **Rescuers**: Publish multiple pet pages
  - **Adopters**: Request pet adoptions

## Tech Stack

- **Backend**: Kotlin, Ktor
- **Database**: H2 (file-based)
- **Auth**: WebAuthn4J (FIDO2/Passkeys)
- **Frontend**: Kotlin HTML (kotlinx.html DSL), JavaScript, SCSS

## Requirements

- Java 17+
- Modern browser with WebAuthn support (Chrome, Firefox, Edge, Safari)

## Run

```bash
./gradlew run
```

Then open http://localhost:8080

**Note**: WebAuthn requires HTTPS or localhost. For production, use HTTPS.

## Project Structure

```
src/main/
├── kotlin/com/adoptu/
│   ├── Application.kt
│   ├── auth/WebAuthnService.kt, SessionUser.kt
│   ├── db/DatabaseFactory.kt
│   ├── di/AppModule.kt
│   ├── models/Models.kt
│   ├── pages/HtmlPages.kt
│   ├── plugins/Serialization, Sessions, Routing, WebAuthn
│   └── repositories/PetRepository.kt
└── resources/
    └── static/           # JS, CSS (HTML from Kotlin)
        ├── scss/style.scss
        └── js/api.js, webauthn.js
```

## Pet Page Fields

Each pet has: name, type, description, weight, age (years/months), and pictures placeholder.

# Application Start Test

A test has been added at `src/test/kotlin/com/adoptu/ApplicationStartTest.kt` to confirm the application starts properly. To run the test, use:

```
./gradlew test --tests com.adoptu.ApplicationStartTest
```

If the test fails, check the error output for details and ensure all dependencies and environment variables are correctly configured.


Deploy:
Retrieve an authentication token and authenticate your Docker client to your registry. Use the AWS CLI:
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 174000857825.dkr.ecr.us-east-1.amazonaws.com
Note: If you receive an error using the AWS CLI, make sure that you have the latest version of the AWS CLI and Docker installed.
Build your Docker image using the following command. For information on building a Docker file from scratch see the instructions here . You can skip this step if your image is already built:
docker build -t production/adoptu .
After the build completes, tag your image so you can push the image to this repository:
docker tag production/adoptu:latest 174000857825.dkr.ecr.us-east-1.amazonaws.com/production/adoptu:latest
Run the following command to push this image to your newly created AWS repository:
docker push 174000857825.dkr.ecr.us-east-1.amazonaws.com/production/adoptu:latest