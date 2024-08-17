package nobleidl.compiler

import esexpr.*
import nobleidl.compiler.format.PackageMapping

final case class ScalaJSLanguageOptions(
  @keyword
  outputDir: String,
  
  @keyword
  packageMapping: PackageMapping,
) derives ESExprCodec

