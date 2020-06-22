val ktor_version: String by project
val test_containers_version: String by project

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    implementation(project(":rpc"))
    testImplementation("org.testcontainers:testcontainers:$test_containers_version")
    testImplementation("org.testcontainers:junit-jupiter:$test_containers_version")
}

idea.module {
}

val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    shouldRunAfter("test")

    useJUnitPlatform() {
        includeTags("integration")
    }

    dependsOn(":echo:createContainer")
}

