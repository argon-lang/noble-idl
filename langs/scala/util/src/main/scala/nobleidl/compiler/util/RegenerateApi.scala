package nobleidl.compiler.util

import dev.argon.esexpr.KeywordMapping
import esexpr.Dictionary
import nobleidl.compiler.backend.{PackageImportMapping, PackageMapping, ScalaJSLanguageOptions, ScalaLanguageOptions}
import nobleidl.compiler.*
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import scala.jdk.CollectionConverters.*
import java.nio.file.Path

object RegenerateApi extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    for
      coreLib <- ZIO.readFile(Path.of("../noble-idl/runtime/nobleidl-core.nidl").nn)
      compilerApi <- ZIO.readFile(Path.of("../noble-idl/backend/compiler-api.nidl").nn)
      scalaAnns <- ZIO.readFile(Path.of("../noble-idl/backend/compiler-api-scala-annotations.nidl").nn)
      javaAnns <- ZIO.readFile(Path.of("../noble-idl/backend/compiler-api-java-annotations.nidl").nn)

      _ <- ScalaPlatformRunner.compile(ScalaIDLCompilerOptions(
        outputDir = Path.of("runtime/jvm/src/gen/scala"),
        languageOptions = ScalaLanguageOptions(
          packageMapping = PackageMapping(Dictionary(Map(
            "nobleidl.core" -> "nobleidl.core",
          ))),
          javaAdapters = Some(ScalaLanguageOptions.JavaAdapters(
            packageMapping = PackageMapping(Dictionary(Map(
              "nobleidl.core" -> "dev.argon.nobleidl.runtime",
            ))),
          )),
          jsAdapters = None,
        ),
        inputFileData = Seq(coreLib),
        libraryFileData = Seq(),
      ))

      _ <- ScalaPlatformRunner.compile(ScalaIDLCompilerOptions(
        outputDir = Path.of("runtime/js/src/gen/scala"),
        languageOptions = ScalaLanguageOptions(
          packageMapping = PackageMapping(Dictionary(Map(
            "nobleidl.core" -> "nobleidl.core",
          ))),
          javaAdapters = None,
          jsAdapters = Some(ScalaLanguageOptions.JSAdapters(
            packageMapping = PackageMapping(Dictionary(Map(
              "nobleidl.core" -> "nobleidl.sjs.core",
            ))),
          )),
        ),
        inputFileData = Seq(coreLib),
        libraryFileData = Seq(),
      ))

      _ <- ScalaJSPlatformRunner.compile(ScalaIDLCompilerOptions(
        outputDir = Path.of("runtime/js/src/gen/scala"),
        languageOptions = ScalaJSLanguageOptions(
          packageMapping = PackageMapping(Dictionary(Map(
            "nobleidl.core" -> "nobleidl.sjs.core",
          ))),
          packageImportMapping = PackageImportMapping(Map())
        ),
        inputFileData = Seq(coreLib),
        libraryFileData = Seq(),
      ))

      _ <- ScalaPlatformRunner.compile(ScalaIDLCompilerOptions(
        outputDir = Path.of("backend-api/src/main/scala"),
        languageOptions = ScalaLanguageOptions(
          packageMapping = PackageMapping(Dictionary(Map(
            "nobleidl.core" -> "nobleidl.core",
            "nobleidl.compiler.api" -> "nobleidl.compiler.api",
            "nobleidl.compiler.api.java" -> "nobleidl.compiler.api.java",
          ))),
          javaAdapters = Some(ScalaLanguageOptions.JavaAdapters(
            packageMapping = PackageMapping(Dictionary(Map(
              "nobleidl.core" -> "dev.argon.nobleidl.core",
              "nobleidl.compiler.api" -> "dev.argon.nobleidl.compiler.api",
              "nobleidl.compiler.api.java" -> "dev.argon.nobleidl.compiler.api.java",
            )))
          )),
          jsAdapters = None,
        ),
        inputFileData = Seq(compilerApi, javaAnns),
        libraryFileData = Seq(coreLib),
      ))

      _ <- ScalaPlatformRunner.compile(ScalaIDLCompilerOptions(
        outputDir = Path.of("backend-api/src/main/scala"),
        languageOptions = ScalaLanguageOptions(
          packageMapping = PackageMapping(Dictionary(Map(
            "nobleidl.core" -> "nobleidl.core",
            "nobleidl.compiler.api" -> "nobleidl.compiler.api",
            "nobleidl.compiler.api.java" -> "dev.argon.nobleidl.compiler.api.java",
            "nobleidl.compiler.api.scala" -> "nobleidl.compiler.api.scalaAnns",
          ))),
          javaAdapters = None,
          jsAdapters = None,
        ),
        inputFileData = Seq(scalaAnns),
        libraryFileData = Seq(coreLib, compilerApi, javaAnns),
      ))
    yield ()
}
