import com.ludd.backend_framework.projectConfig
import org.springframework.boot.gradle.tasks.bundling.BootJar

repositories {
    mavenCentral()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

plugins {
    idea
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("com.bmuschko.docker-remote-api") version "6.4.0"
}

group = "com.ludd"
version = "0.0.1"

projectConfig()

tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

val ktor_version: String by project

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    implementation("org.reflections:reflections:0.9.12")
    implementation("org.litote.kmongo:kmongo:4.1.0")
    implementation("org.litote.kmongo:kmongo-coroutine:4.1.0")
}

idea.module {
}

tasks.create("buildImage", com.bmuschko.gradle.docker.tasks.image.DockerBuildImage::class) {
    inputDir.set(file("./docker"))
    dockerFile.set(file("./docker/primary.dockerfile"))
    images.add("mongo-replica-set:4.4")
    images.add("mongo-replica-set:latest")
    group = "docker"
}