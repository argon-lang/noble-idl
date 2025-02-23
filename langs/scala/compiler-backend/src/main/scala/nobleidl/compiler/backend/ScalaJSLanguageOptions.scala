package nobleidl.compiler.backend

import esexpr.*

final case class ScalaJSLanguageOptions(
  packageMapping: PackageMapping,
  packageImportMapping: PackageImportMapping,
)

