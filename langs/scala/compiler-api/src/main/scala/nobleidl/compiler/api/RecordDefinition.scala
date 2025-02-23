package nobleidl.compiler.api
@_root_.esexpr.constructor("record-definition")
final case class RecordDefinition(
  @_root_.esexpr.vararg
  fields: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.RecordField],
  @_root_.esexpr.keyword("esexpr-options")
  @_root_.esexpr.optional
  esexprOptions: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.EsexprRecordOptions],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object RecordDefinition {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.RecordDefinition, _root_.dev.argon.nobleidl.compiler.api.RecordDefinition] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.RecordDefinition, _root_.dev.argon.nobleidl.compiler.api.RecordDefinition] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.RecordDefinition): _root_.dev.argon.nobleidl.compiler.api.RecordDefinition = {
        new _root_.dev.argon.nobleidl.compiler.api.RecordDefinition(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.RecordField, _root_.dev.argon.nobleidl.compiler.api.RecordField](_root_.nobleidl.compiler.api.RecordField.javaAdapter()).toJava(s_value.fields),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprRecordOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordOptions](_root_.nobleidl.compiler.api.EsexprRecordOptions.javaAdapter()).toJava(s_value.esexprOptions),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.RecordDefinition): _root_.nobleidl.compiler.api.RecordDefinition = {
        _root_.nobleidl.compiler.api.RecordDefinition(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.RecordField, _root_.dev.argon.nobleidl.compiler.api.RecordField](_root_.nobleidl.compiler.api.RecordField.javaAdapter()).fromJava(j_value.fields().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprRecordOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordOptions](_root_.nobleidl.compiler.api.EsexprRecordOptions.javaAdapter()).fromJava(j_value.esexprOptions().nn),
        )
      }
    }
}
