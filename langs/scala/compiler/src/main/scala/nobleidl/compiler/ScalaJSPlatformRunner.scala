package nobleidl.compiler

import esexpr.Dictionary
import nobleidl.compiler.ScalaNobleIDLCompiler.Config
import nobleidl.compiler.api.NobleIdlGenerationRequest
import nobleidl.compiler.backend.{Backend, PackageImportMapping, PackageMapping, ScalaJSBackend, ScalaJSLanguageOptions}

private[compiler] object ScalaJSPlatformRunner extends PlatformRunner {
  override val platform: String = "scalajs"
  override type LanguageOptions = ScalaJSLanguageOptions

  private[compiler] override def shouldEnable(config: Config): Boolean =
    config.generateScalaJS

  private[compiler] override def languageOptions(config: Config, libRes: LibraryAnalyzer.LibraryResults, currentLibRes: LibraryAnalyzer.LibraryResults): ScalaJSLanguageOptions =
    ScalaJSLanguageOptions(
      packageMapping = PackageMapping(
        mapping = Dictionary(
          libRes.scalaJSPackageMapping ++ currentLibRes.scalaJSPackageMapping
        ),
      ),
      packageImportMapping = PackageImportMapping(
        libRes.scalaJSImportMapping ++ currentLibRes.scalaJSImportMapping
      ),
    )

  protected override def createBackend(request: NobleIdlGenerationRequest[ScalaJSLanguageOptions]): Backend =
    ScalaJSBackend(request)
}
