package nobleidl.compiler.api
enum EsexprRecordFieldKind derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("positional")
  case Positional(
    mode: _root_.nobleidl.compiler.api.EsexprRecordPositionalMode,
  )
  @_root_.esexpr.constructor("keyword")
  case Keyword(
    name: _root_.nobleidl.core.String,
    mode: _root_.nobleidl.compiler.api.EsexprRecordKeywordMode,
  )
  @_root_.esexpr.constructor("dict")
  case Dict(
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
  )
  @_root_.esexpr.constructor("vararg")
  case Vararg(
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
  )
}
