import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.linker.interface.ESVersion
import sbt.util

import java.io.File
import java.nio.charset.StandardCharsets
import scala.sys.process.Process


val zioVersion = "2.1.9"

ThisBuild / organization := "dev.argon.nobleidl"
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / resolvers += Resolver.mavenLocal
ThisBuild / evictionErrorLevel := Level.Warn

publish / skip := true

val buildSettings = Seq(
  scalaVersion := "3.5.1",
  scalacOptions ++= Seq(
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
  ),
)

lazy val runtime = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Full).in(file("runtime"))
  .jvmSettings(
    libraryDependencies += "dev.argon.nobleidl" % "nobleidl-java-runtime" % "0.1.0-SNAPSHOT",
  )
  .settings(
    buildSettings,

    Compile / packageBin / packageOptions += Package.ManifestAttributes("Automatic-Module-Name" -> "dev.argon.nobleidl.core.scala"),

    libraryDependencies  ++= Seq(
      "dev.argon.esexpr" %%% "esexpr-scala-runtime" % "0.1.1-SNAPSHOT",
    ),
    
    Compile / unmanagedSourceDirectories ++= Seq(
      baseDirectory.value / "../shared/src/gen/scala",
      baseDirectory.value / "../shared/src/main/java",
    ),

    name := "nobleidl-scala-runtime",
  )

lazy val runtimeJVM = runtime.jvm
lazy val runtimeJS = runtime.js

lazy val backend = project.in(file("backend"))
  .dependsOn(runtimeJVM)
  .settings(
    buildSettings,

    Compile / unmanagedSourceDirectories += baseDirectory.value / "src/gen/scala",

    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),

    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio" % zioVersion,
      "dev.zio" %%% "zio-streams" % zioVersion,
      "dev.zio" %%% "zio-interop-cats" % "23.1.0.3",

      "dev.zio" %%% "zio-test" % zioVersion % "test",
      "dev.zio" %%% "zio-test-sbt" % zioVersion % "test",

      "com.github.scopt" %%% "scopt" % "4.1.0",
      "org.apache.commons" % "commons-text" % "1.12.0",
      "org.ow2.asm" % "asm" % "9.7",

      "dev.argon.jawawasm" % "wasm-engine" % "0.1.0",
      "dev.argon" %%% "argon-async-util" % "0.1.1-SNAPSHOT",
      "dev.argon.nobleidl" % "nobleidl-java-compiler" % "0.1.0-SNAPSHOT",
    ),

    Compile / resourceGenerators += Def.task {
      val outputFile = (Compile / resourceManaged).value / "nobleidl/compiler/noble-idl-compiler.wasm"

      val cargoProject = "noble-idl-compiler"
      val cargoDirectory = baseDirectory.value / "../../.."
      val compiledFile = cargoDirectory / "target/wasm32-unknown-unknown/release" / (cargoProject.replace("-", "_") + ".wasm")


      val exitCode = Process(
        Seq("cargo", "build", "-p", cargoProject, "--target=wasm32-unknown-unknown", "--release"),
        cargoDirectory,
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
    buildSettings,
    publish / skip := true,

    run / fork := true,
    run / baseDirectory := file("."),
  )

val example = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Full).in(file("example"))
  .dependsOn(runtime)
  .jvmSettings(

    Compile / unmanagedJars += baseDirectory.value / "../../../java/example/build/libs/example.jar",
    libraryDependencies += "org.jetbrains" % "annotations" % "24.0.0",
  )
  .settings(
    buildSettings,
    publish / skip := true,

    Compile / sourceGenerators += Def.task {
      val s = streams.value
      val baseDir = baseDirectory.value
      val managedDir = (Compile / sourceManaged).value
      val resourceDir = (Compile / resourceManaged).value

      val inputDir = baseDir / "../shared/src/main/nobleidl"
      val sourceDirs = (Compile / unmanagedSourceDirectories).value

      val javaArgs = javaOptions.value
      val generatorCP = (backend / Compile / fullClasspath).value.map(_.data.toString).mkString(File.pathSeparator)
      val depCP = (Compile / dependencyClasspath).value

      val f = FileFunction.cached(s.cacheDirectory / "generate-nobleidl") { (in: Set[File]) =>
        val outDir = managedDir / "generate-nobleidl"
        IO.delete(outDir)
        IO.createDirectory(outDir)

        s.log.info(s"Generating sources from NobleIDL schema in ${outDir}")

        IO.createDirectory(outDir)
        val exitCode = Process(
          Seq("java") ++
            javaArgs ++
            Seq(
              "-cp",
              generatorCP,
              "nobleidl.compiler.ScalaNobleIDLCompiler",
              "--scala",
              "--input",
              inputDir.toString,
            ) ++
            sourceDirs.filter(_.exists()).flatMap(dir => Seq("--java-source", dir.toString)) ++
            Seq(
              "--output",
              outDir.toString,
              "--resource-output",
              resourceDir.toString,
            ) ++
            depCP.flatMap { f => Seq("--library", f.data.toString) } ++
            (
              crossProjectPlatform.value match {
                case JVMPlatform =>
                  Seq(
                    "--java-adapters",
                  )

                case JSPlatform =>
                  Seq(
                    "--js-adapters",
                    "--scalajs",
                  )

                case _ => Seq()
              }
            )
        ).!(s.log)

        if (exitCode != 0) {
          throw new Exception(s"ESExpr Generator failed with exit code ${exitCode}")
        }

        outDir.allPaths.filter(_.isFile).get().toSet
      }

      val inputFiles = inputDir.allPaths.filter(f => f.isFile && f.getName.endsWith(".nidl")).get().toSet

      f(inputFiles).toSeq
    },

    name := "nobleidl-scala-example",
  )

lazy val exampleJVM = example.jvm
lazy val exampleJS = example.js


lazy val sbtPlugin = project.in(file("sbt-plugin"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-nobleidl",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.9.0"
      }
    },

    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-release", "22",
      "-language:higherKinds",
      "-language:existentials",
      "-language:implicitConversions",
    ),
  )

