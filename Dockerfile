# Build stage - using Amazon Corretto 25 with Dart Sass
FROM docker.io/amazoncorretto:25 AS builder

WORKDIR /app

# Install required tools and Dart Sass
RUN dnf install -y tar gzip findutils \
    && curl -fsSL https://github.com/sass/dart-sass/releases/download/1.77.8/dart-sass-1.77.8-linux-x64.tar.gz -o /tmp/sass.tar.gz \
    && mkdir -p /opt/dart-sass \
    && tar -xzf /tmp/sass.tar.gz -C /opt/dart-sass \
    && ln -sf /opt/dart-sass/dart-sass/sass /usr/local/bin/sass \
    && rm /tmp/sass.tar.gz \
    && dnf clean all

COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

RUN chmod +x gradlew
RUN ./gradlew compileKotlin compileSass processResources --no-daemon
RUN ./gradlew shadowJar --no-daemon

# Runtime stage - using Alpine for small image
FROM docker.io/amazoncorretto:25-alpine-jdk

WORKDIR /app

COPY --from=builder /app/build/libs/*-all.jar app.jar
RUN mkdir -p /app/static/css && cp /app/build/sass/* /app/static/css/ 2>/dev/null || true
COPY src/main/resources/application.conf .

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV ADOPTU_ENV="prod"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
