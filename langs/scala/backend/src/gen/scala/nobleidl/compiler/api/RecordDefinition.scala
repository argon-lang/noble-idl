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
}
