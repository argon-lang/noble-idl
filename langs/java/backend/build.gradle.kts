import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

plugins {
    java
    application
    `java-library`
    `maven-publish`
    signing
}

group = "dev.argon.nobleidl"
version = "0.1.0-SNAPSHOT"

application {
    mainClass = "dev.argon.nobleidl.compiler.JavaNobleIDLCompiler"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }

    sourceSets["main"].java.srcDir("src/gen/java")
    sourceSets["main"].resources.srcDir("build/generated/wasm")
}

dependencies {
    implementation(libs.jetbrains.annotations)
    implementation(libs.jawawasm.engine)
    implementation(libs.apache.commons.text)
    implementation(libs.apache.commons.cli)
    implementation(libs.asm)
    api(libs.esexpr.runtime)
    annotationProcessor(libs.esexpr.generator)
    api(project(":runtime"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "nobleidl-java-compiler"
            from(components["java"])

            pom {
                name = "Noble IDL Java Compiler"
                description = "Noble IDL Compiler for Java"
                url = "https://github.com/argon-lang/nobleidl"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        name = "argon-dev"
                        email = "argon@argon.dev"
                        organization = "argon-lang"
                        organizationUrl = "https://argon.dev"
                    }
                }
                scm {
                    connection = "scm:git:git@github.com:argon-lang/esexpr.git"
                    developerConnection = "scm:git:git@github.com:argon-lang/esexpr.git"
                    url = "https://github.com/argon-lang/nobleidl/tree/master/langs/java"
                }
            }
        }
    }

    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}


abstract class GenerateRustWasm : DefaultTask() {

    @get: Input
    abstract val cargoDirectory: Property<File>

    @get: Input
    abstract val cargoProject: Property<String>

    @get: Input
    abstract val outputFile: Property<File>

    @TaskAction
    fun buildWasm() {
        Files.createDirectories(outputFile.get().parentFile.toPath())

        val pb = ProcessBuilder(listOf("cargo", "build", "-p", cargoProject.get(), "--target=wasm32-unknown-unknown", "--release"))
        pb.directory(cargoDirectory.get())
        val process = pb.start()
        val exitCode = process.waitFor()
        if(exitCode != 0) {
            throw Exception("Cargo failed with exit code $exitCode")
        }

        Files.copy(
            cargoDirectory.get().toPath().resolve("target/wasm32-unknown-unknown/release").resolve(cargoProject.get().replace("-", "_") + ".wasm"),
            outputFile.get().toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

}


tasks.register<GenerateRustWasm>("generateWasmNobleIDLCompiler") {
    cargoDirectory = projectDir.resolve("../../..")
    cargoProject = "noble-idl-compiler"
    outputFile = projectDir.resolve("build/generated/wasm/dev/argon/nobleidl/compiler/noble-idl-compiler.wasm")
}

tasks.withType<JavaCompile> {
    dependsOn(tasks.getByName("generateWasmNobleIDLCompiler"))
}



