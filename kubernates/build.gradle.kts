repositories {
    mavenCentral()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

plugins {
}

group = "com.ludd"
version = "0.0.1"

dependencies {
}

val startKubernates = tasks.create<Exec>("startKubernates") {
    executable = "kubectl"
    args("apply", "-f", "../kubernates")
    group="kubernates"
    dependsOn(":echo:dockerBuildImage")
    dependsOn(":gateway:dockerBuildImage")
    dependsOn(":player:dockerBuildImage")
    dependsOn(":mongo:buildImage")
}

val stopKubernates = tasks.create<Exec>("stopKubernates") {
    executable = "kubectl"
    args("delete", "all", "--all")  //TODO: delete not everything, but only those created in startKubernates
    group="kubernates"
    mustRunAfter(":player:integrationTest") //TODO: move to other modules gradle files
    mustRunAfter(":gateway:integrationTest")
}