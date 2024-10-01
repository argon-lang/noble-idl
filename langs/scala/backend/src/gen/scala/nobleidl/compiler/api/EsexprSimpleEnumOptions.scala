package nobleidl.compiler.api
@_root_.esexpr.constructor("simple-enum-options")
final case class EsexprSimpleEnumOptions(
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprSimpleEnumOptions {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprSimpleEnumOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumOptions] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprSimpleEnumOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumOptions] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprSimpleEnumOptions): _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumOptions = {
        new _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumOptions(
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumOptions): _root_.nobleidl.compiler.api.EsexprSimpleEnumOptions = {
        _root_.nobleidl.compiler.api.EsexprSimpleEnumOptions(
        )
      }
    }
}
