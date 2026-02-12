plugins {
    kotlin("jvm") version "2.3.0"
    application
}

group = "com.softpaw.systems"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-network:3.0.3")
    implementation("io.ktor:ktor-network-tls:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("io.arrow-kt:arrow-core:2.0.0")
    implementation("ch.qos.logback:logback-classic:1.5.16")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:6.0.0")
    testImplementation("io.kotest:kotest-assertions-core:6.0.0")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.softpaw.systems.MainKt")
}

tasks.test {
    useJUnitPlatform()
}