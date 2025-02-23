package nobleidl.compiler.api
@_root_.esexpr.constructor("enum-options")
final case class EsexprEnumOptions(
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprEnumOptions {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprEnumOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprEnumOptions] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprEnumOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprEnumOptions] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprEnumOptions): _root_.dev.argon.nobleidl.compiler.api.EsexprEnumOptions = {
        new _root_.dev.argon.nobleidl.compiler.api.EsexprEnumOptions(
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprEnumOptions): _root_.nobleidl.compiler.api.EsexprEnumOptions = {
        _root_.nobleidl.compiler.api.EsexprEnumOptions(
        )
      }
    }
}
