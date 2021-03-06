import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.ludd.backend_framework.projectConfig

plugins {
    idea
    id("com.bmuschko.docker-java-application") version "6.4.0"
    id("com.bmuschko.docker-remote-api") version "6.4.0"
}

projectConfig()

idea.module {
    //sourceDirs.add(file(protobuf.protobuf.generatedFilesBaseDir + "/main/grpc"))
}

docker {
    javaApplication {
        baseImage.set("openjdk:13")
        maintainer.set("Anatoly Ahmedov 'ludd@bk.ru'")
        ports.set(listOf(9000, 9000))
        images.set(setOf("echo_grpc:0.1", "echo_grpc:latest"))
        jvmArgs.set(listOf("-Xms256m", "-Xmx2048m"))
    }
}

val dockerTask = tasks.named<Dockerfile>("dockerCreateDockerfile") {
}

val createMyAppContainer by tasks.creating(DockerCreateContainer::class) {
    dependsOn(dockerTask)
    targetImageId("echo_grpc")
    hostConfig.portBindings.set(listOf("9000:9000"))
    hostConfig.autoRemove.set(true)
}

val startMyAppContainer by tasks.creating(DockerStartContainer::class) {
    dependsOn(createMyAppContainer)
    targetContainerId(createMyAppContainer.containerId)
}

val stopMyAppContainer by tasks.creating(DockerStopContainer::class) {
    targetContainerId(createMyAppContainer.containerId)
}

tasks {
    named("integrationTest") {
        dependsOn(startMyAppContainer)
        finalizedBy(stopMyAppContainer)
    }
}
