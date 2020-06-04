java.sourceCompatibility = JavaVersion.VERSION_11
val ktor_version: String by project
val kotlin_version: String by project
val spring_boot_version: String by project
val protobuf_version: String by project

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}

tasks {
    clean {
        delete(protobuf.protobuf.generatedFilesBaseDir)
    }
}

idea.module {

}

