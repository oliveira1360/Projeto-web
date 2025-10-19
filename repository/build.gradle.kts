plugins {
    kotlin("jvm") version "2.2.10"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":domain"))

    implementation("org.jdbi:jdbi3-core:3.37.1")
    implementation("org.jdbi:jdbi3-kotlin:3.37.1")
    implementation("org.jdbi:jdbi3-postgres:3.37.1")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("org.postgresql:postgresql:42.6.0")
}

tasks.test {
    useJUnitPlatform()
    dependsOn("dbTestsUp", "dbTestsWait")
    finalizedBy("dbTestsDown")
}
kotlin {
    jvmToolchain(21)
}

val composeFileDir: Directory = rootProject.layout.projectDirectory
val dockerComposePath: String = composeFileDir.file("docker/docker-compose.yml").asFile.absolutePath

val dockerExe =
    when (
        org.gradle.internal.os.OperatingSystem
            .current()
    ) {
        org.gradle.internal.os.OperatingSystem.MAC_OS -> "/usr/local/bin/docker"
        org.gradle.internal.os.OperatingSystem.WINDOWS -> "docker"
        else -> "docker" // Linux and others
    }
tasks.register<Exec>("dbTestsUp") {
    commandLine(dockerExe, "compose", "-f", dockerComposePath, "up", "-d", "--build", "--force-recreate", "db-tests")
}

tasks.register("dbTestsWait") {
    dependsOn("dbTestsUp")

    doLast {
        println("Waiting for PostgreSQL to be ready...")
        Thread.sleep(15000)
        println("PostgreSQL should be ready now")
    }
}

tasks.register<Exec>("dbTestsDown") {
    commandLine(dockerExe, "compose", "-f", dockerComposePath, "down", "db-tests")
}
