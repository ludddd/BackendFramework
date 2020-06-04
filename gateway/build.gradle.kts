import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    idea
}

java.sourceCompatibility = JavaVersion.VERSION_11
val ktor_version: String by project
val kotlin_version: String by project
val spring_boot_version: String by project
val protobuf_version: String by project

dependencies {
    implementation("org.springframework.boot:spring-boot-starter:$spring_boot_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$spring_boot_version") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    implementation("io.github.microutils:kotlin-logging:1.7.9")

    implementation("com.google.protobuf:protobuf-gradle-plugin:0.8.12")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    implementation("com.google.protobuf:protobuf-java:3.12.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xuse-experimental=kotlin.Experimental")
        jvmTarget = "1.8"
    }
}

protobuf {
    generatedFilesBaseDir = "$projectDir/gen"
    protoc {
        artifact = "com.google.protobuf:protoc:3.12.2"
    }
    generateProtoTasks {
    }
}

tasks {
    clean {
        delete(protobuf.protobuf.generatedFilesBaseDir)
    }
}

idea.module {

}

