plugins {
    kotlin("jvm") version "2.2.10"
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":service"))
    api(project(":utils"))

    implementation("org.springframework.boot:spring-boot-starter-web:3.5.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // for JDBI and Postgres
    implementation("org.jdbi:jdbi3-core:3.37.1")
    implementation("org.postgresql:postgresql:42.7.2")

    // To use WebTestClient in integration tests with real HTTP server
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.6")

    // To automatically run the Spring MVC web server in coordination with unit tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}