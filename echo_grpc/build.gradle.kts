java.sourceCompatibility = JavaVersion.VERSION_11

tasks {
    clean {
        delete(protobuf.protobuf.generatedFilesBaseDir)
    }
}

idea.module {
    sourceDirs.add(file(protobuf.protobuf.generatedFilesBaseDir + "/main/grpc"))
}