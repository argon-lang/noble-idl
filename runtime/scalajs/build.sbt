


lazy val runtime = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion := "3.4.1",
    libraryDependencies += "dev.zio" %%% "zio" % "2.1.1",

    scalacOptions ++= Seq(
        "-Yexplicit-nulls",
    ),
  )

