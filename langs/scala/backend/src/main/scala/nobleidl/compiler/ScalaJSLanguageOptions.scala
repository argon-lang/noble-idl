package nobleidl.compiler

import esexpr.*
import nobleidl.compiler.PackageMapping

final case class ScalaJSLanguageOptions(
  packageMapping: PackageMapping,
  packageImportMapping: PackageImportMapping,
)

