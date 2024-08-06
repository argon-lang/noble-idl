plugins {
    java
    application
    `java-library`
}

application {
    mainClass = "dev.argon.nobleidl.compiler.RegenerateApi"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.jvmwasm.engine)
    implementation(libs.apache.commons.text)
    annotationProcessor(libs.esexpr.generator)
    api(libs.esexpr.runtime)
    api(project(":runtime"))
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
