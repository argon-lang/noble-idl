package nobleidl.compiler.api
@_root_.esexpr.constructor("field-options")
final case class EsexprRecordFieldOptions(
  kind: _root_.nobleidl.compiler.api.EsexprRecordFieldKind,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprRecordFieldOptions {
}
