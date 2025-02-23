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
object RecordField {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.RecordField, _root_.dev.argon.nobleidl.compiler.api.RecordField] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.RecordField, _root_.dev.argon.nobleidl.compiler.api.RecordField] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.RecordField): _root_.dev.argon.nobleidl.compiler.api.RecordField = {
        new _root_.dev.argon.nobleidl.compiler.api.RecordField(
          _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
          _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.fieldType),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).toJava(s_value.annotations),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprRecordFieldOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions](_root_.nobleidl.compiler.api.EsexprRecordFieldOptions.javaAdapter()).toJava(s_value.esexprOptions),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.RecordField): _root_.nobleidl.compiler.api.RecordField = {
        _root_.nobleidl.compiler.api.RecordField(
          _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
          _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.fieldType().nn),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).fromJava(j_value.annotations().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprRecordFieldOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions](_root_.nobleidl.compiler.api.EsexprRecordFieldOptions.javaAdapter()).fromJava(j_value.esexprOptions().nn),
        )
      }
    }
}
