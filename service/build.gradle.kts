plugins {
    alias(libs.plugins.kotlin.jvm)
    kotlin("plugin.spring") version "1.9.25"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":repository"))

    implementation("org.springframework:spring-webmvc:6.2.10")

    // To use PreDestroy annotation
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // To use SLF4J
    implementation("org.slf4j:slf4j-api:2.0.16")

    // For dependency injection
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    // To get password encode
    api("org.springframework.security:spring-security-core:6.5.5")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set("1.5.0")
}

kotlin {
    jvmToolchain(21)
}
