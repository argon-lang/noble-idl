import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.linker.interface.ESVersion

import scala.sys.process.Process


val zioVersion = "2.0.21"

ThisBuild / resolvers += Resolver.mavenLocal

ThisBuild / organization := "dev.argon"
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.2"
ThisBuild / scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-release", "22",
  "-source", "future",
  "-language:higherKinds",
  "-language:existentials",
  "-language:implicitConversions",
  "-language:strictEquality",
  "-deprecation",
  "-feature",
  "-Ycheck-all-patmat",
  "-Yretain-trees",
  "-Yexplicit-nulls",
  "-Xmax-inlines", "128",
  "-Wconf:id=E029:e,id=E165:e,id=E190:e,cat=unchecked:e,cat=deprecation:e",
)

lazy val runtime = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).in(file("runtime"))
  .settings(
    libraryDependencies  ++= Seq(
      "dev.argon.esexpr" %%% "esexpr-scala-runtime" % "0.1.0-SNAPSHOT",
    ),

    name := "nobleidl-scala-runtime",
  )

lazy val runtimeJVM = runtime.jvm
lazy val runtimeJS = runtime.js

lazy val backend = project.in(file("backend"))
  .dependsOn(runtimeJVM)
  .settings(

    Compile / unmanagedSourceDirectories += baseDirectory.value / "src/gen/scala",

    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),

    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio" % zioVersion,
      "dev.zio" %%% "zio-streams" % zioVersion,
      "dev.zio" %%% "zio-interop-cats" % "23.1.0.3",

      "dev.zio" %%% "zio-test" % zioVersion % "test",
      "dev.zio" %%% "zio-test-sbt" % zioVersion % "test",

      "com.github.scopt" %%% "scopt" % "4.1.0",

      "dev.argon.jawawasm" % "wasm-engine" % "0.1.0",
      "org.apache.commons" % "commons-text" % "1.12.0",
    ),

    Compile / resourceGenerators += Def.task {
      val outputFile = (Compile / resourceManaged).value / "nobleidl/compiler/noble-idl-compiler.wasm"

      val cargoProject = "noble-idl-compiler"
      val cargoDirectory = baseDirectory.value / "../../.."
      val compiledFile = cargoDirectory / "target/wasm32-unknown-unknown/release" / (cargoProject.replace("-", "_") + ".wasm")


      val exitCode = Process(
        Seq("cargo", "build", "-p", cargoProject, "--target=wasm32-unknown-unknown", "--release"),
        cargoDirectory,
        "RUSTFLAGS" -> "-C target-feature=+multivalue",
      ).!
      if(exitCode != 0) {
        throw new RuntimeException(s"Cargo failed with exit code $exitCode")
      }


      IO.copyFile(compiledFile, outputFile)

      Seq(outputFile)
    },

    name := "nobleidl-scala-compiler",
  )

val util = project.in(file("util"))
  .dependsOn(backend)
  .settings(
    run / fork := true,
    run / baseDirectory := file("."),
  )


