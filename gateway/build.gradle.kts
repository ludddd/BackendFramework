val ktor_version: String by project

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}

idea.module {
}

