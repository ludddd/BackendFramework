import com.ludd.backend_framework.projectConfig

repositories {
    mavenCentral()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

plugins {
    idea
    kotlin("jvm")
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
}

idea.module {
}

tasks {
    named("integrationTest") {
        dependsOn(":echo:dockerBuildImage")
    }
}


