package nobleidl.compiler.api
@_root_.esexpr.constructor("simple-enum-case-options")
final case class EsexprSimpleEnumCaseOptions(
  name: _root_.nobleidl.core.String,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprSimpleEnumCaseOptions {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions): _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions = {
        new _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions(
          _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions): _root_.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions = {
        _root_.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions(
          _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
        )
      }
    }
}
