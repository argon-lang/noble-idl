package nobleidl.compiler

import esexpr.*
import nobleidl.compiler.format.PackageMapping

final case class ScalaJSLanguageOptions(
  outputDir: String,
  packageMapping: PackageMapping,
  packageImportMapping: PackageImportMapping,
)

