package nobleidl.compiler.api
enum TypeExpr derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("defined-type")
  case DefinedType(
    name: _root_.nobleidl.compiler.api.QualifiedName,
    @_root_.esexpr.vararg
    args: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.TypeExpr],
  )
  @_root_.esexpr.constructor("type-parameter")
  case TypeParameter(
    name: _root_.nobleidl.core.String,
  )
}
