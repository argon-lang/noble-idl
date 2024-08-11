package nobleidl.compiler

import nobleidl.compiler.format.PackageMapping
import esexpr.*

final case class ScalaLanguageOptions(
  @keyword
  outputDir: String,
  
  @keyword
  packageMapping: PackageMapping,
) derives ESExprCodec

