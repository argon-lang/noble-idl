package nobleidl.compiler

import dev.argon.nobleidl.compiler.JavaLanguageOptions
import nobleidl.compiler.backend.{Backend, WrappedJavaBackend}
import nobleidl.compiler.api.NobleIdlGenerationRequest
import nobleidl.compiler.ScalaNobleIDLCompiler.Config
import scala.jdk.CollectionConverters.given

private[compiler] object JavaPlatformRunner extends PlatformRunner {
  override val platform: String = "java"
  override type LanguageOptions = JavaLanguageOptions

  private[compiler] override def shouldEnable(config: Config): Boolean =
    config.generateJava

  private[compiler] override def languageOptions(config: Config, libRes: LibraryAnalyzer.LibraryResults, currentLibRes: LibraryAnalyzer.LibraryResults): JavaLanguageOptions =
    JavaLanguageOptions(
      dev.argon.nobleidl.compiler.PackageMapping(
        dev.argon.esexpr.KeywordMapping(
          (libRes.javaPackageMapping ++ currentLibRes.javaPackageMapping).asJava
        )
      ),
      config.graaljsAdapters
    )

  override protected def createBackend(request: NobleIdlGenerationRequest[JavaLanguageOptions]): Backend =
    WrappedJavaBackend(dev.argon.nobleidl.compiler.JavaBackend(
      NobleIdlGenerationRequest.javaAdapter(nobleidl.core.JavaAdapter.identity)
        .toJava(request)
    ))
}
