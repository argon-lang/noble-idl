package nobleidl.compiler.api
enum EsexprEnumCaseType derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("constructor")
  case Constructor(
    name: _root_.nobleidl.core.String,
  )
  @_root_.esexpr.constructor("inline-value")
  case InlineValue(
  )
}
object EsexprEnumCaseType {
}
