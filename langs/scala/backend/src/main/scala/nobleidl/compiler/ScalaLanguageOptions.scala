package nobleidl.compiler

import nobleidl.compiler.PackageMapping
import esexpr.*
import nobleidl.compiler.ScalaLanguageOptions.{JSAdapters, JavaAdapters}

final case class ScalaLanguageOptions(  
  @keyword
  packageMapping: PackageMapping,

  @keyword
  @optional
  javaAdapters: Option[JavaAdapters],
  
  @keyword
  @optional
  jsAdapters: Option[JSAdapters],
) derives ESExprCodec

object ScalaLanguageOptions {
  final case class JavaAdapters(
    @keyword 
    packageMapping: PackageMapping
  ) derives ESExprCodec
  
  final case class JSAdapters(
    @keyword
    packageMapping: PackageMapping
  ) derives ESExprCodec
}

