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

val launchKubernates = tasks.create<Exec>("launchKubernates") {
    executable = "kubectl"
    args("apply", "-f", "../kubernates")    //this command doesn't wait for kubernates to actually launch everything
    group="kubernates"
    dependsOn(":echo:dockerBuildImage")
    dependsOn(":gateway:dockerBuildImage")
    dependsOn(":player:dockerBuildImage")
    dependsOn(":mongo:buildImage")
    dependsOn(":fail:dockerBuildImage")
}

//this tasks just wait for kubernates to start all pods launched by launchKubernates task
val startKubernates = tasks.create<Exec>("startKubernates") {
    executable = "kubectl"
    args("wait", "--for=condition=Ready", "--all=true", "--timeout=2m", "pods")
    group="kubernates"
    dependsOn("launchKubernates")
}

val stopKubernates = tasks.create<Exec>("stopKubernates") {
    executable = "kubectl"
    args("delete", "all", "--all")  //TODO: delete not everything, but only those created in startKubernates
    group="kubernates"
}