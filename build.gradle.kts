import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

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
	kotlin("plugin.spring") version "1.3.70" apply(false)
	idea
}

idea.project {
	languageLevel = IdeaLanguageLevel(11)
	targetBytecodeVersion = JavaVersion.VERSION_11
}


