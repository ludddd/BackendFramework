group = "com.ludd"
version = "0.0.1"

subprojects {
	version = "0.0.1"
}

allprojects {
	repositories {
		mavenCentral()
		mavenLocal()
		jcenter()
		maven { url = uri("https://kotlin.bintray.com/ktor") }
	}
}


