import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.linker.interface.ESVersion

import java.io.File
import java.nio.charset.StandardCharsets
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

lazy val runtime = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Full).in(file("runtime"))
  .jvmSettings(
    Compile / unmanagedJars += baseDirectory.value / "../../../java/runtime/build/libs/runtime.jar",
  )
  .settings(
    libraryDependencies  ++= Seq(
      "dev.argon.esexpr" %%% "esexpr-scala-runtime" % "0.1.0-SNAPSHOT",
    ),
    
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src/gen/scala",

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

val example = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Full).in(file("example"))
  .dependsOn(runtime)
  .jvmSettings(
    Compile / unmanagedJars += baseDirectory.value / "../../../java/example/build/libs/example.jar",
    libraryDependencies += "org.jetbrains" % "annotations" % "24.0.0",
  )
  .settings(
    Compile / sourceGenerators += Def.task {
      val s = streams.value
      val baseDir = baseDirectory.value
      val managedDir = (Compile / sourceManaged).value
      val resourceDir = (Compile / resourceManaged).value

      val inputDir = baseDir / "../shared/src/main/nobleidl"

      val javaArgs = javaOptions.value
      val generatorCP = (backend / Compile / fullClasspath).value.map(_.data.toString).mkString(File.pathSeparator)
      val depCP = (Compile / dependencyClasspath).value

      val genRunner = (Compile / runner).value

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
              "--output",
              outDir.toString,
              "--resource-output",
              resourceDir.toString,
              "--package-mapping",
              "nobleidl.example=nobleidl.example",
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
                    "--js-package-mapping",
                    "nobleidl.example=nobleidl.sjs.example",
                    "--js-package-import",
                    "nobleidl.example=@argon-lang/noble-idl-example",
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
