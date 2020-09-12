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
    implementation(project(":mongo"))
    implementation("org.litote.kmongo:kmongo:4.1.0")
    implementation("org.litote.kmongo:kmongo-coroutine:4.1.0")
    testImplementation("org.testcontainers:testcontainers:$test_containers_version")
    testImplementation("org.testcontainers:junit-jupiter:$test_containers_version")
    testImplementation(project(":test_utils"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
}

idea.module {
}

docker {
    javaApplication {
        baseImage.set("openjdk:13")
        maintainer.set("Anatoly Ahmedov 'ludd@bk.ru'")
        ports.set(listOf(9001, 9001))
        images.set(setOf("ludd.player:0.1", "ludd.player:latest"))
        jvmArgs.set(listOf("-Xms256m", "-Xmx2048m"))
    }
}

tasks {
    named("integrationTest") {
        dependsOn("startKubernates")
    }
}

val startKubernates = tasks.create<Exec>("startKubernates") {
    executable = "kubectl"
    args("apply", "-f", "../kubernates")
    group="kubernates"
    dependsOn(":echo:dockerBuildImage")
    dependsOn(":player:dockerBuildImage")
    dependsOn(":gateway:dockerBuildImage")
}

val stopKubernates = tasks.create<Exec>("stopKubernates") {
    executable = "kubectl"
    args("delete", "all", "--all")  //TODO: delete not everything, but only those created in startKubernates
    group="kubernates"
    mustRunAfter("integrationTest")
}





