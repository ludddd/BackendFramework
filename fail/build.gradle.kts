import com.ludd.backend_framework.projectConfig

group = "com.ludd"
version = "0.0.1"

repositories {
    mavenCentral()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

plugins {
    id("com.bmuschko.docker-java-application") version "6.4.0"
    id("com.bmuschko.docker-remote-api") version "6.4.0"
    idea
    kotlin("jvm")// version "1.3.70"
}

projectConfig()

val ktor_version: String by project
val test_containers_version: String by project

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    implementation(project(":rpc"))
    testImplementation("org.testcontainers:testcontainers:$test_containers_version")
    testImplementation("org.testcontainers:junit-jupiter:$test_containers_version")
    testImplementation(project(":test_utils"))
    testImplementation("io.kubernetes:client-java:10.0.0")
}

idea.module {
}

docker {
    javaApplication {
        baseImage.set("openjdk:13")
        maintainer.set("Anatoly Ahmedov 'ludd@bk.ru'")
        ports.set(listOf(9001, 9001))
        images.set(setOf("ludd.fail:0.1", "ludd.fail:latest"))
        jvmArgs.set(listOf("-Xms256m", "-Xmx2048m"))
    }
}

tasks {
    named("integrationTest") {
        dependsOn(":kubernates:startKubernates")
        finalizedBy(":kubernates:stopKubernates")
    }
}





