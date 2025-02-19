package nobleidl.compiler

import esexpr.ESExprCodec


enum ScalaLanguageExternTypeAnnotations derives ESExprCodec, CanEqual {
  case AdapterNeedsZioRuntime
}