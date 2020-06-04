java.sourceCompatibility = JavaVersion.VERSION_11

idea.module {
    sourceDirs.add(file(protobuf.protobuf.generatedFilesBaseDir + "/main/grpc"))
}