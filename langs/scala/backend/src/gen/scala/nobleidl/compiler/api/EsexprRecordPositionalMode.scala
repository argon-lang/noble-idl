package nobleidl.compiler.api
enum EsexprRecordPositionalMode derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("required")
  case Required(
  )
  @_root_.esexpr.constructor("optional")
  case Optional(
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
  )
}
object EsexprRecordPositionalMode {
}
