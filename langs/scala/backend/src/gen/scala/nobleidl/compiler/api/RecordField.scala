package nobleidl.compiler.api
@_root_.esexpr.constructor("record-field")
final case class RecordField(
  name: _root_.nobleidl.core.String,
  fieldType: _root_.nobleidl.compiler.api.TypeExpr,
  @_root_.esexpr.keyword("annotations")
  annotations: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.Annotation],
  @_root_.esexpr.keyword("esexpr-options")
  @_root_.esexpr.optional
  esexprOptions: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.EsexprRecordFieldOptions],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
