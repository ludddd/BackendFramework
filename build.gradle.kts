group = "com.ludd"
version = "0.0.1"

subprojects {
	version = "0.0.1"
}

repositories {
	mavenCentral()
}

plugins {
	id("org.springframework.boot") version "2.3.0.RELEASE" apply(false)
	id("io.spring.dependency-management") version "1.0.9.RELEASE" apply(false)
	kotlin("jvm")
	kotlin("plugin.spring") version "1.3.70" apply(false)
	id("com.google.protobuf")
}
