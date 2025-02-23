package nobleidl.compiler

import esexpr.Dictionary
import nobleidl.compiler.ScalaNobleIDLCompiler.Config
import nobleidl.compiler.api.NobleIdlGenerationRequest
import nobleidl.compiler.backend.{Backend, PackageMapping, ScalaLanguageOptions, ScalaBackend}

object ScalaPlatformRunner extends PlatformRunner {
  override val platform: String = "scala"
  override type LanguageOptions = ScalaLanguageOptions

  private[compiler] override def shouldEnable(config: Config): Boolean =
    config.generateScala

  private[compiler] override def languageOptions(config: Config, libRes: LibraryAnalyzer.LibraryResults, currentLibRes: LibraryAnalyzer.LibraryResults): ScalaLanguageOptions =
    ScalaLanguageOptions(
      packageMapping = PackageMapping(Dictionary(
        libRes.scalaPackageMapping ++ currentLibRes.scalaPackageMapping
      )),
      javaAdapters =
        if config.javaAdapters then
          Some(ScalaLanguageOptions.JavaAdapters(
            packageMapping = PackageMapping(Dictionary(
              libRes.javaPackageMapping ++ currentLibRes.javaPackageMapping
            ))
          ))
        else
          None,
      jsAdapters =
        if config.jsAdapters then
          Some(ScalaLanguageOptions.JSAdapters(
            packageMapping = PackageMapping(Dictionary(
              libRes.scalaJSPackageMapping ++ currentLibRes.scalaJSPackageMapping
            ))
          ))
        else
          None,
    )

  protected override def createBackend(request: NobleIdlGenerationRequest[ScalaLanguageOptions]): Backend =
    ScalaBackend(request)
}
