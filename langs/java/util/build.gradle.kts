import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":backend"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}


tasks.register<JavaExec>("regenerateApi") {
    dependsOn(tasks.withType<JavaCompile>())

    mainClass = "dev.argon.nobleidl.util.RegenerateApi"
    classpath = java.sourceSets["main"].runtimeClasspath
}
