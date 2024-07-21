
scalaVersion := "3.4.1"

libraryDependencies += "dev.zio" %% "zio" % "2.1.1"


lazy val nobidl_core = ProjectRef(file(sys.env("NOBIDL_ROOT_DIR") + "/runtime/scala_zio"), "runtimeJVM")

lazy val root = project.in(file("."))
    .dependsOn(nobidl_core)

