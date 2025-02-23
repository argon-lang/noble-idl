package nobleidl.compiler.api
@_root_.esexpr.constructor("record-options")
final case class EsexprRecordOptions(
  @_root_.esexpr.keyword("constructor")
  constructor: _root_.nobleidl.core.String,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprRecordOptions {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprRecordOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordOptions] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprRecordOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordOptions] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprRecordOptions): _root_.dev.argon.nobleidl.compiler.api.EsexprRecordOptions = {
        new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordOptions(
          _root_.nobleidl.core.String.javaAdapter().toJava(s_value.constructor),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordOptions): _root_.nobleidl.compiler.api.EsexprRecordOptions = {
        _root_.nobleidl.compiler.api.EsexprRecordOptions(
          _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.constructor().nn),
        )
      }
    }
}
