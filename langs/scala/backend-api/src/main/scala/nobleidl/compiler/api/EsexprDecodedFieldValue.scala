package nobleidl.compiler.api
@_root_.esexpr.constructor("field-value")
final case class EsexprDecodedFieldValue(
  name: _root_.nobleidl.core.String,
  value: _root_.nobleidl.compiler.api.EsexprDecodedValue,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprDecodedFieldValue {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedFieldValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedFieldValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprDecodedFieldValue): _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue = {
        new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue(
          _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
          _root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter().toJava(s_value.value),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue): _root_.nobleidl.compiler.api.EsexprDecodedFieldValue = {
        _root_.nobleidl.compiler.api.EsexprDecodedFieldValue(
          _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
          _root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter().fromJava(j_value.value().nn),
        )
      }
    }
}
