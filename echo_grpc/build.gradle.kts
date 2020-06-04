java.sourceCompatibility = JavaVersion.VERSION_11
val kotlin_version: String by project
val spring_boot_version: String by project
val protobuf_version: String by project
val grpc_version: String by project

tasks {
    clean {
        delete(protobuf.protobuf.generatedFilesBaseDir)
    }
}

idea.module {
    sourceDirs.add(file(protobuf.protobuf.generatedFilesBaseDir + "/main/grpc"))
}