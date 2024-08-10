import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.linker.interface.ESVersion


val zioVersion = "2.0.21"

ThisBuild / resolvers += Resolver.mavenLocal

lazy val backend = project.in(file("backend"))
  .settings(
    scalaVersion := "3.4.2",

    organization := "dev.argon",
    version := "0.1.0-SNAPSHOT",

    Compile / unmanagedJars ++= Seq(
      file("../java/backend/build/libs/backend.jar"),
      file("../java/runtime/build/libs/runtime.jar"),
    ),

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


    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),

    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio" % zioVersion,
      "dev.zio" %%% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-interop-cats" % "23.1.0.3",

      "dev.zio" %%% "zio-test" % zioVersion % "test",
      "dev.zio" %%% "zio-test-sbt" % zioVersion % "test",

      "com.github.scopt" %%% "scopt" % "4.1.0",

      "dev.argon.jvmwasm" % "wasm-engine" % "0.1.0",
      "dev.argon.esexpr" %%% "esexpr-scala-runtime" % "0.1.0-SNAPSHOT",
      "dev.argon" % "esexpr-java-runtime" % "0.1.0",
      "dev.argon" % "esexpr-java-runtime" % "0.1.0",
      "org.apache.commons" % "commons-text" % "1.12.0",
      "commons-cli" % "commons-cli" % "1.8.0",
    ),

    name := "nobleidl-scala-compiler",
  )

