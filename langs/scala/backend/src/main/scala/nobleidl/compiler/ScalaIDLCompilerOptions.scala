package nobleidl.compiler

import java.nio.file.Path

final case class ScalaIDLCompilerOptions[L](
  outputDir: Path,
  languageOptions: L,
  inputFileData: Seq[String],
  libraryFileData: Seq[String],
)
