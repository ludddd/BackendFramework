idea.module {
    sourceDirs.add(file(protobuf.protobuf.generatedFilesBaseDir + "/main/grpc"))
}

val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    shouldRunAfter("test")

    useJUnitPlatform() {
        includeTags("integration")
    }
}

tasks.check { dependsOn(integrationTest) }