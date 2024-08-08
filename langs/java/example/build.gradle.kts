import java.nio.charset.StandardCharsets

plugins {
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api(project(":runtime"))
    api(libs.esexpr.runtime)
    annotationProcessor(libs.esexpr.generator)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }

    sourceSets["main"].java.srcDir("build/generated/sources/nobleidl")
    sourceSets["main"].resources.srcDir("build/generated/resources/nobleidl")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register("codegenNobleIDL") {
    dependsOn(project(":backend").tasks.jar)

    val packageMapping = mapOf(
        "nobleidl.example" to "dev.argon.nobleidl.example",
    )

    val backendJar = project(":backend").tasks.jar.get().archiveFile.get().asFile

    inputs.files(backendJar)

    val sourceDir = projectDir.resolve("src/main/nobleidl")
    inputs
        .files(fileTree(sourceDir) {
            include("**/*.nidl")
        })
        .skipWhenEmpty()
        .withPropertyName("sourceFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .ignoreEmptyDirectories()

    val outputDir = projectDir.resolve("build/generated/sources/nobleidl")
    outputs.dir(layout.buildDirectory.dir("generated/sources/nobleidl")).withPropertyName("outputDir")

    val resOutputDir = projectDir.resolve("build/generated/resources/nobleidl")
    outputs.dir(layout.buildDirectory.dir("generated/resources/nobleidl")).withPropertyName("resOutputDir")

    doLast {
        val javaExe = File(System.getProperty("java.home"), "bin/java").toString()

        val backendProjModules = objects.fileCollection()
        backendProjModules.setFrom(backendJar)

        val modulePath = project(":backend")
            .sourceSets["main"]
            .runtimeClasspath
            .filter { it.isFile && it.toString().endsWith(".jar") }
            .plus(backendProjModules)
            .asPath

        val args = mutableListOf(javaExe, "--module-path", modulePath, "--module", "dev.argon.nobleidl.compiler/dev.argon.nobleidl.compiler.JavaNobleIDLCompiler")

        for(mapping in packageMapping) {
            args.add("--package-mapping")
            args.add("${mapping.key}=${mapping.value}")
        }

        args.add("--input")
        args.add(sourceDir.toString())

        args.add("--output")
        args.add(outputDir.toString())

        args.add("--resource-output")
        args.add(resOutputDir.toString())

        val runtimeProjModules = objects.fileCollection()
        runtimeProjModules.setFrom(project(":runtime").tasks.jar.get().archiveFile.get().asFile)

        sourceSets["main"].compileClasspath
            .filter { it.isFile && it.toString().endsWith(".jar") }
            .plus(runtimeProjModules)
            .files
            .forEach { file ->
                args.add("--java-library")
                args.add(file.toString())
            }

        outputDir.deleteRecursively()
        resOutputDir.deleteRecursively()

        val pb = ProcessBuilder(args)
        pb.directory(projectDir)
        pb.redirectError()
        val process = pb.start()
        val exitCode = process.waitFor()
        if(exitCode != 0) {
            val stdout = String(process.inputStream.readAllBytes(), StandardCharsets.UTF_8)
            val stderr = String(process.errorStream.readAllBytes(), StandardCharsets.UTF_8)
            throw Exception("NobleIDL code generation failed with exit code $exitCode\n$stdout\n$stderr")
        }
    }
}

tasks.withType<JavaCompile> {
    dependsOn(tasks.getByName("codegenNobleIDL"))
}

tasks.withType<ProcessResources> {
    dependsOn(tasks.getByName("codegenNobleIDL"))
}
