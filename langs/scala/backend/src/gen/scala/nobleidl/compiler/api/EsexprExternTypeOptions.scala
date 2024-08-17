package nobleidl.compiler.api
@_root_.esexpr.constructor("extern-type-options")
final case class EsexprExternTypeOptions(
  @_root_.esexpr.keyword("allow-value")
  allowValue: _root_.nobleidl.core.Bool = _root_.nobleidl.core.Bool.fromBool(false),
  @_root_.esexpr.keyword("allow-optional")
  @_root_.esexpr.optional
  allowOptional: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.TypeExpr],
  @_root_.esexpr.keyword("allow-vararg")
  @_root_.esexpr.optional
  allowVararg: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.TypeExpr],
  @_root_.esexpr.keyword("allow-dict")
  @_root_.esexpr.optional
  allowDict: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.TypeExpr],
  @_root_.esexpr.keyword("literals")
  literals: _root_.nobleidl.compiler.api.EsexprExternTypeLiterals,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprExternTypeOptions {
}
