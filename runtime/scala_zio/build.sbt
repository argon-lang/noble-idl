

lazy val runtime = crossProject(JVMPlatform, JSPlatform).in(file("."))
    .settings(
        scalaVersion := "3.4.1",
        libraryDependencies += "dev.zio" %%% "zio" % "2.1.1",
        scalacOptions ++= Seq(
            "-Yexplicit-nulls",
        ),
    )

lazy val runtimeJVM = runtime.jvm
lazy val runtimeJS = runtime.js

