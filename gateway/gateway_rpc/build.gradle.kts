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

dependencies {
}

idea.module {
}