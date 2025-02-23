package nobleidl.compiler.backend

import esexpr.ESExprCodec


enum ScalaLanguageExternTypeAnnotations derives ESExprCodec, CanEqual {
  case AdapterNeedsZioRuntime
}