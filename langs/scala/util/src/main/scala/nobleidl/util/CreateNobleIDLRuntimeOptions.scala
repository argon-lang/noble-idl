package nobleidl.util

import esexpr.{Dictionary, ESExprBinaryDecoder, ESExprBinaryEncoder, ESExprCodec}
import nobleidl.compiler.format.{BackendMapping, BackendOptions, NobleIdlJarOptions, PackageMapping}
import zio.stream.ZSink
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import java.nio.file.{Files, Path}

object CreateNobleIDLRuntimeOptions extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =

    def buildOptions(backendMap: Map[String, BackendOptions]): NobleIdlJarOptions =
      NobleIdlJarOptions(
        idlFiles = Seq("nobleidl/core/nobleidl-core.nidl"),
        backends = BackendMapping(Dictionary(backendMap)),
      )

    val scalaBackends = Map(
      "scala" -> BackendOptions(
        packageMapping = PackageMapping(Dictionary(Map(
          "nobleidl.core" -> "nobleidl.core",
        ))),
      ),
    )

    val jvmBackends = scalaBackends
    val jsBackends = scalaBackends ++ Map(
      "scalajs" -> BackendOptions(
        packageMapping = PackageMapping(Dictionary(Map(
          "nobleidl.core" -> "nobleidl.sjs.core",
        ))),
      ),
    )

    def generate(platform: String, options: NobleIdlJarOptions): ZIO[Any, Throwable, Unit] =
      ESExprBinaryEncoder.writeWithSymbolTable(summon[ESExprCodec[NobleIdlJarOptions]].encode(options))
        .run(ZSink.fromPath(Path.of(s"runtime/$platform/src/main/resources/nobleidl-options.esxb").nn))
        .unit

    generate("jvm", buildOptions(jvmBackends)) &>
      generate("js", buildOptions(jsBackends))
  end run

}
