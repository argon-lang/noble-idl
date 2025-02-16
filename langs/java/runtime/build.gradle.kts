import org.w3c.dom.Node

plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "dev.argon.nobleidl"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    api(libs.esexpr.runtime)
    annotationProcessor(libs.esexpr.generator)
    compileOnly(libs.graal.polyglot)
    compileOnly(libs.graal.js)
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "nobleidl-java-runtime"
            from(components["java"])

            pom {
                name = "Noble IDL Java Runtime"
                description = "Noble IDL Java Runtime Library"
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

                withXml {

                    val rootElement = asElement()
                    val ownerDocument = rootElement.ownerDocument

                    val depsNode = asElement().getElementsByTagName("dependencies").item(0)

                    for(lib in listOf(libs.graal.polyglot.get(), libs.graal.js.get())) {
                        val depElem = depsNode.ownerDocument.createElement("dependency")
                        depsNode.appendChild(depElem)

                        depElem.appendChild(ownerDocument.createElement("groupId")).textContent = lib.group
                        depElem.appendChild(ownerDocument.createElement("artifactId")).textContent = lib.name
                        depElem.appendChild(ownerDocument.createElement("version")).textContent = lib.version
                        depElem.appendChild(ownerDocument.createElement("scope")).textContent = "optional"
                    }
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

