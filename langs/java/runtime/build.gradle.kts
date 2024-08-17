plugins {
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.jetbrains.annotations)
    api(libs.esexpr.runtime)
    annotationProcessor(libs.esexpr.generator)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }

    sourceSets["main"].java.srcDir("src/gen/java")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

