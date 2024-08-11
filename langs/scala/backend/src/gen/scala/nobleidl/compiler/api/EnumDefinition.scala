package nobleidl.compiler.api
@_root_.esexpr.constructor("enum-definition")
final case class EnumDefinition(
  @_root_.esexpr.vararg
  cases: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.EnumCase],
  @_root_.esexpr.keyword("esexpr-options")
  @_root_.esexpr.optional
  esexprOptions: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.EsexprEnumOptions],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
