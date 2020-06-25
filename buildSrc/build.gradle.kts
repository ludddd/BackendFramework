plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

val kotlin_version: String by project

dependencies {
    //"implementation"("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    //"implementation"("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    "implementation"("com.google.protobuf:protobuf-java:3.12.2")
    //"implementation"("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
}