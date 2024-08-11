package nobleidl.compiler.api
@_root_.esexpr.constructor("enum-case")
final case class EnumCase(
  name: _root_.nobleidl.core.String,
  @_root_.esexpr.vararg
  fields: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.RecordField],
  @_root_.esexpr.keyword("esexpr-options")
  @_root_.esexpr.optional
  esexprOptions: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.EsexprEnumCaseOptions],
  @_root_.esexpr.keyword("annotations")
  annotations: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.Annotation],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
