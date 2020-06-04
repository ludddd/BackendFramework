group = "com.ludd"
version = "0.0.1"

subprojects {
	version = "0.0.1"
}

plugins {
	id("org.springframework.boot") version "2.3.0.RELEASE" apply(false)
	id("io.spring.dependency-management") version "1.0.9.RELEASE" apply(false)
	kotlin("jvm") version "1.3.72" apply(false)
	kotlin("plugin.spring") version "1.3.72" apply(false)
	id("com.google.protobuf") version "0.8.12" apply(false)
}

val kotlin_version: String by project
val spring_boot_version: String by project
val protobuf_version: String by project
val grpc_version: String by project

subprojects {
	repositories {
		mavenCentral()
		mavenLocal()
		jcenter()
		maven { url = uri("https://kotlin.bintray.com/ktor") }
	}

	apply(plugin = "org.springframework.boot")
	apply(plugin = "io.spring.dependency-management")
	apply(plugin = "com.google.protobuf")
	apply(plugin = "org.jetbrains.kotlin.jvm")
	apply(plugin = "org.jetbrains.kotlin.plugin.spring")
	apply(plugin = "org.gradle.idea")

	dependencies {
		"implementation"("org.springframework.boot:spring-boot-starter:$spring_boot_version")
		"implementation"("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
		"implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
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