package nobleidl.util

import dev.argon.esexpr.KeywordMapping
import esexpr.Dictionary
import nobleidl.compiler.{ScalaIDLCompilerOptions, ScalaLanguageOptions}
import nobleidl.compiler.format.PackageMapping
import nobleidl.compiler.ScalaNobleIDLCompiler
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import scala.jdk.CollectionConverters.*
import java.nio.file.Path

object RegenerateApi extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    for
      coreLib <- ZIO.readFile(Path.of("../noble-idl/runtime/nobleidl-core.nidl").nn)
      compilerApi <- ZIO.readFile(Path.of("../noble-idl/backend/compiler-api.nidl").nn)
      jarMetadata <- ZIO.readFile(Path.of("../noble-idl/backend/jar-metadata.nidl").nn)

      _ <- ScalaNobleIDLCompiler.ScalaPlatformRunner.compile(ScalaIDLCompilerOptions(
        languageOptions = ScalaLanguageOptions(
          outputDir = "backend/src/gen/scala",
          packageMapping = PackageMapping(Dictionary(Map(
            "nobleidl.core" -> "nobleidl.core",
            "nobleidl.compiler.api" -> "nobleidl.compiler.api",
            "nobleidl.compiler.jar-metadata" -> "nobleidl.compiler.format",
          ))),
        ),
        inputFileData = Seq(compilerApi, jarMetadata),
        libraryFileData = Seq(coreLib),
      ))
    yield ()
}
