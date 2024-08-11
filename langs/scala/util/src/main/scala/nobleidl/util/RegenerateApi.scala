package nobleidl.util

import dev.argon.esexpr.KeywordMapping
import dev.argon.nobleidl.compiler.{JavaIDLCompilerOptions, JavaLanguageOptions, PackageMapping}
import nobleidl.compiler.ScalaNobleIDLCompiler
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import scala.jdk.CollectionConverters.*
import java.nio.file.Path

object RegenerateApi extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    for
      coreLib <- ZIO.readFile(Path.of("../noble-idl/runtime/nobleidl-core.nidl").nn)
      compilerApi <- ZIO.readFile(Path.of("../noble-idl/backend/compiler-api.nidl").nn)

      _ <- ScalaNobleIDLCompiler.compile(JavaIDLCompilerOptions(
        JavaLanguageOptions(
          "backend/src/gen/scala",
          PackageMapping(KeywordMapping(Map(
            "nobleidl.core" -> "nobleidl.core",
            "nobleidl.compiler.api" -> "nobleidl.compiler.api",
          ).asJava)),
        ),
        Seq(compilerApi).asJava,
        Seq(coreLib).asJava,
      ))
    yield ()
}