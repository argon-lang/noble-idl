package nobleidl.compiler

final case class ScalaIDLCompilerOptions[L](
  languageOptions: L,
  inputFileData: Seq[String],
  libraryFileData: Seq[String],
)
