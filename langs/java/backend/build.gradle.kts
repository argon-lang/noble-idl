import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

plugins {
    java
    application
    `java-library`
}

application {
    mainClass = "dev.argon.nobleidl.compiler.JavaNobleIDLCompiler"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.jetbrains.annotations)
    implementation(libs.jawawasm.engine)
    implementation(libs.apache.commons.text)
    implementation(libs.apache.commons.cli)
    api(libs.esexpr.runtime)
    annotationProcessor(libs.esexpr.generator)
    api(project(":runtime"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }

    sourceSets["main"].java.srcDir("src/gen/java")
    sourceSets["main"].resources.srcDir("build/generated/wasm")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
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
        pb.environment().set("RUSTFLAGS", "-C target-feature=+multivalue")
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



