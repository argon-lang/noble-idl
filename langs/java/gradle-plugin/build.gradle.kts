plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "dev.argon.nobleidl"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":backend"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

gradlePlugin {
    plugins {
        create("nobleIdlCodeGen") {
            id = "dev.argon.nobleidl"
            implementationClass = "dev.argon.nobleidl.gradleplugin.NobleIDLCodeGenPlugin"
            version = "0.1.0-SNAPSHOT"
            description = "Generates Java sources from NobleIDL definitions"
            displayName = "NobleIDL Code Generation Gradle Plugin"
        }
    }
}

//publishing {
//    publications {
//        create<MavenPublication>("mavenJava") {
//            artifactId = "dev.argon.nobleidl.gradle.plugin"
//            from(components["java"])
//
//            pom {
//                name = "Noble IDL Gradle Plugin"
//                description = "Gradle Plugin for Noble IDL"
//                url = "https://github.com/argon-lang/nobleidl"
//                licenses {
//                    license {
//                        name = "Apache License, Version 2.0"
//                        url = "https://www.apache.org/licenses/LICENSE-2.0"
//                    }
//                }
//                developers {
//                    developer {
//                        name = "argon-dev"
//                        email = "argon@argon.dev"
//                        organization = "argon-lang"
//                        organizationUrl = "https://argon.dev"
//                    }
//                }
//                scm {
//                    connection = "scm:git:git@github.com:argon-lang/esexpr.git"
//                    developerConnection = "scm:git:git@github.com:argon-lang/esexpr.git"
//                    url = "https://github.com/argon-lang/nobleidl/tree/master/langs/java"
//                }
//            }
//        }
//    }
//
//    repositories {
//        maven {
//            url = uri(layout.buildDirectory.dir("repo"))
//        }
//    }
//}
//
//signing {
//    sign(publishing.publications["mavenJava"])
//}
