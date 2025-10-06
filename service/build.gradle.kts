plugins {
    kotlin("jvm") version "2.2.10"
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":repository"))

    // For dependency injection
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    // To get password encode
    api("org.springframework.security:spring-security-core:6.5.5")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}