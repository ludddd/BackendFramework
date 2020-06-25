package com.ludd.backend_framework

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.*

class CommonConfigPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        apply(plugin = "org.springframework.boot")
        apply(plugin = "io.spring.dependency-management")
        apply(plugin = "com.google.protobuf")
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "org.jetbrains.kotlin.plugin.spring")
        apply(plugin = "org.gradle.idea")

        val kotlin_version: String by project
        val spring_boot_version: String by project
        val protobuf_version: String by project
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

        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}