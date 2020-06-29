plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

val kotlin_version: String by project

dependencies {
    compileOnly(gradleApi())
    implementation(kotlin("gradle-plugin"))
    //"implementation"("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    //"implementation"("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlin_version")
    "implementation"("com.google.protobuf:protobuf-gradle-plugin:0.8.12")
    "implementation"("com.google.protobuf:protobuf-java:3.12.2")
}

/*kotlinDslPluginOptions {
    experimentalWarning.set(false)
}*/



