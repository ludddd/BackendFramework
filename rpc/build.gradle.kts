import org.springframework.boot.gradle.tasks.bundling.BootJar

group = "com.ludd"
version = "0.0.1"

tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}