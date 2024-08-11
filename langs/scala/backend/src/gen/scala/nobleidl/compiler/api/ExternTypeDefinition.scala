package nobleidl.compiler.api
@_root_.esexpr.constructor("extern-type-definition")
final case class ExternTypeDefinition(
  @_root_.esexpr.keyword("esexpr-options")
  @_root_.esexpr.optional
  esexprOptions: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.EsexprExternTypeOptions],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
