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
}