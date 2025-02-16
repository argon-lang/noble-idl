import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

dependencies {
    api(project(":backend"))
}


tasks.register<JavaExec>("regenerateApi") {
    dependsOn(tasks.withType<JavaCompile>())

    mainClass = "dev.argon.nobleidl.util.RegenerateApi"
    classpath = java.sourceSets["main"].runtimeClasspath
}
