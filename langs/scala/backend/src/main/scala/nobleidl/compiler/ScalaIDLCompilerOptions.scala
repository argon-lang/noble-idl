package nobleidl.compiler

final case class ScalaIDLCompilerOptions(
  languageOptions: ScalaLanguageOptions,
  inputFileData: Seq[String],
  libraryFileData: Seq[String],
)
