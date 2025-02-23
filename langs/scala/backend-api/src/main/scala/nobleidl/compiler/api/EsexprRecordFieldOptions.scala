package nobleidl.compiler.api
@_root_.esexpr.constructor("field-options")
final case class EsexprRecordFieldOptions(
  kind: _root_.nobleidl.compiler.api.EsexprRecordFieldKind,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprRecordFieldOptions {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprRecordFieldOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprRecordFieldOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprRecordFieldOptions): _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions = {
        new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions(
          _root_.nobleidl.compiler.api.EsexprRecordFieldKind.javaAdapter().toJava(s_value.kind),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldOptions): _root_.nobleidl.compiler.api.EsexprRecordFieldOptions = {
        _root_.nobleidl.compiler.api.EsexprRecordFieldOptions(
          _root_.nobleidl.compiler.api.EsexprRecordFieldKind.javaAdapter().fromJava(j_value.kind().nn),
        )
      }
    }
}
