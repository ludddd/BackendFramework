import com.ludd.backend_framework.projectConfig

repositories {
    mavenCentral()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

plugins {
    idea
    kotlin("jvm")
    id("com.bmuschko.docker-java-application") version "6.4.0"
    id("com.bmuschko.docker-remote-api") version "6.4.0"
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
    testImplementation("io.kubernetes:client-java:8.0.2")
}

idea.module {
}

tasks {
    named("integrationTest") {
        dependsOn(":echo:dockerBuildImage")
        dependsOn("startKubernates")
    }
}

docker {
    javaApplication {
        baseImage.set("openjdk:13")
        maintainer.set("Anatoly Ahmedov 'ludd@bk.ru'")
        ports.set(listOf(9000, 9000))
        images.set(setOf("ludd.gateway:0.1", "ludd.gateway:latest"))
        jvmArgs.set(listOf("-Xms256m", "-Xmx2048m"))
    }
}

val startKubernates = tasks.create<Exec>("startKubernates") {
    executable = "kubectl"
    args("apply", "-f", "../kubernates")
    group="kubernates"
    dependsOn(":echo:dockerBuildImage")
    dependsOn("dockerBuildImage")
}

val stopKubernates = tasks.create<Exec>("stopKubernates") {
    executable = "kubectl"
    args("delete", "all", "--all")
    group="kubernates"
    shouldRunAfter("integrationTest")
}




