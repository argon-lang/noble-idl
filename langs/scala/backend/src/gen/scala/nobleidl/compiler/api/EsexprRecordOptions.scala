package nobleidl.compiler.api
@_root_.esexpr.constructor("record-options")
final case class EsexprRecordOptions(
  @_root_.esexpr.keyword("constructor")
  constructor: _root_.nobleidl.core.String,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprRecordOptions {
}
