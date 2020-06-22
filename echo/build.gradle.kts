import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.Dockerfile

group = "com.ludd"
version = "0.0.1"

val ktor_version: String by project

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    implementation(project(":rpc"))
}

idea.module {
}

plugins {
    id("com.bmuschko.docker-java-application") version "6.4.0"
    id("com.bmuschko.docker-remote-api") version "6.4.0"
}

docker {
    javaApplication {
        baseImage.set("openjdk:13")
        maintainer.set("Anatoly Ahmedov 'ludd@bk.ru'")
        ports.set(listOf(9000, 9000))
        images.set(setOf("echo:0.1", "echo:latest"))
        jvmArgs.set(listOf("-Xms256m", "-Xmx2048m"))
        jvmArgs.set(listOf("-Xms256m", "-Xmx2048m"))
    }
}

val dockerTask = tasks.named<Dockerfile>("dockerCreateDockerfile") {
}

val createContainer by tasks.creating(DockerCreateContainer::class) {
    dependsOn(dockerTask)
    targetImageId("echo")
    hostConfig.portBindings.set(listOf("9000:9000"))
    hostConfig.autoRemove.set(true)
}

val startContainer by tasks.creating(DockerStartContainer::class) {
    dependsOn(createContainer)
    targetContainerId(createContainer.containerId)
}

val stopContainer by tasks.creating(DockerStopContainer::class) {
    targetContainerId(createContainer.containerId)
}
