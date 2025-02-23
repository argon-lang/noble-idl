package nobleidl.compiler.api
@_root_.esexpr.constructor("enum-case-options")
final case class EsexprEnumCaseOptions(
  caseType: _root_.nobleidl.compiler.api.EsexprEnumCaseType,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprEnumCaseOptions {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprEnumCaseOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseOptions] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprEnumCaseOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseOptions] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprEnumCaseOptions): _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseOptions = {
        new _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseOptions(
          _root_.nobleidl.compiler.api.EsexprEnumCaseType.javaAdapter().toJava(s_value.caseType),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseOptions): _root_.nobleidl.compiler.api.EsexprEnumCaseOptions = {
        _root_.nobleidl.compiler.api.EsexprEnumCaseOptions(
          _root_.nobleidl.compiler.api.EsexprEnumCaseType.javaAdapter().fromJava(j_value.caseType().nn),
        )
      }
    }
}
