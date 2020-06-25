plugins {
    `kotlin-dsl`
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("projectConfig") {
            id = "com.ludd.backend-framework"
            implementationClass = "com.ludd.backend_framework.CommonConfigPlugin"
        }
    }
}

/*gradlePlugin {
    plugins {
        register("greet-plugin") {
            id = "greet"
            implementationClass = "GreetPlugin"
        }
    }
}*/

repositories {
    //mavenCentral()
    //mavenLocal()
    jcenter()
}

val kotlin_version: String by project

dependencies {
    //"implementation"("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    //"implementation"("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    "implementation"("com.google.protobuf:protobuf-java:3.12.2")
    "implementation"("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
}