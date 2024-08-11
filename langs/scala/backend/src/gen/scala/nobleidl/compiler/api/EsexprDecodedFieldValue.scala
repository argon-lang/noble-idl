package nobleidl.compiler.api
@_root_.esexpr.constructor("field-value")
final case class EsexprDecodedFieldValue(
  name: _root_.nobleidl.core.String,
  value: _root_.nobleidl.compiler.api.EsexprDecodedValue,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
