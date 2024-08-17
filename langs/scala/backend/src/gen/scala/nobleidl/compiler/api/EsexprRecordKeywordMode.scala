package nobleidl.compiler.api
enum EsexprRecordKeywordMode derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("required")
  case Required(
  )
  @_root_.esexpr.constructor("optional")
  case Optional(
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
  )
  @_root_.esexpr.constructor("default-value")
  case DefaultValue(
    value: _root_.nobleidl.compiler.api.EsexprDecodedValue,
  )
}
object EsexprRecordKeywordMode {
}
