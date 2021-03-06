package com.ludd.backend_framework

import com.google.protobuf.gradle.*
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.projectConfig() {
    addRepositories()
    applyPlugins()
    addDependencies()
    setCompileFlags()
    setupProtoBuf()
    excludeGradleApiFromBuild()
    setupIntegrationTests()
    setJUnit5Tests()
}

private fun Project.setJUnit5Tests() {
    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
}

private fun Project.addRepositories() {
    repositories {
        mavenCentral()
        mavenLocal()
        jcenter()
        maven { url = uri("https://kotlin.bintray.com/ktor") }
    }
}

private fun Project.applyPlugins() {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.google.protobuf")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.gradle.idea")
}

private fun Project.addDependencies() {
    val kotlin_version: String by project
    val spring_boot_version: String by project
    val grpc_version: String by project

    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter:$spring_boot_version")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
        "implementation"("io.github.microutils:kotlin-logging:1.7.9")
        "implementation"("com.google.protobuf:protobuf-gradle-plugin:0.8.12")
        "implementation"("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        "implementation"("com.google.protobuf:protobuf-java:3.12.2")
        "implementation"("io.grpc:grpc-netty-shaded:$grpc_version")
        "implementation"("io.grpc:grpc-protobuf:$grpc_version")
        "implementation"("io.grpc:grpc-stub:$grpc_version")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test:$spring_boot_version") {
            exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        }
    }
}

private fun Project.setCompileFlags() {
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict",
                "-Xuse-experimental=kotlin.Experimental",
                "-Xopt-in=kotlin.ExperimentalStdlibApi")
            jvmTarget = "11"
        }
    }

}

private fun Project.excludeGradleApiFromBuild() {
    //to fix problem with org.slf4j.impl.StaticLoggerBinder.class from gradle api leaking to modules and conflicting
    //with logback engine
    configurations {
        "testCompile" {
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-gradle-plugin")
        }
    }
}

private fun Project.setupProtoBuf() {
    val grpc_version: String by project

    val generatedSrcPath = "$projectDir/gen"
    protobuf {
        generatedFilesBaseDir = generatedSrcPath
        protoc {
            artifact = "com.google.protobuf:protoc:3.12.2"
        }
        plugins {
            // Specify protoc to generate using kotlin protobuf plugin
            id("grpc") {
                artifact = "io.grpc:protoc-gen-grpc-java:$grpc_version"
            }
        }
        generateProtoTasks {
            ofSourceSet("main").forEach {
                it.plugins {
                    // Apply the "grpc" plugin whose spec is defined above, without options.
                    id("grpc")
                }
            }
        }
    }

    tasks.withType<Delete> {
        delete(generatedSrcPath)
    }
}

private fun Project.setupIntegrationTests() {
    val sourceSets = the<SourceSetContainer>()

    sourceSets {
        create("integrationTest") {
            withConvention(KotlinSourceSet::class) {
                kotlin.srcDir("src/integrationTest/kotlin")
                resources.srcDir("src/integrationTest/resources")
                compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
                runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
            }
        }
    }

    task<Test>("integrationTest") {
        description = "Runs the integration tests"
        group = "verification"
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        shouldRunAfter(tasks["test"])
        useJUnitPlatform()
    }

}