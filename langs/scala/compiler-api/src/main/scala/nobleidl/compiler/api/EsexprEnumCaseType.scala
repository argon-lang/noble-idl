package nobleidl.compiler.api
enum EsexprEnumCaseType derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("constructor")
  case Constructor(
    name: _root_.nobleidl.core.String,
  )
  @_root_.esexpr.constructor("inline-value")
  case InlineValue(
  )
}
object EsexprEnumCaseType {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprEnumCaseType, _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseType] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprEnumCaseType, _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseType] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprEnumCaseType): _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseType = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.EsexprEnumCaseType.Constructor =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseType.Constructor(
              _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprEnumCaseType.InlineValue =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseType.InlineValue(
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseType): _root_.nobleidl.compiler.api.EsexprEnumCaseType = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseType.Constructor =>
            new _root_.nobleidl.compiler.api.EsexprEnumCaseType.Constructor(
              _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprEnumCaseType.InlineValue =>
            new _root_.nobleidl.compiler.api.EsexprEnumCaseType.InlineValue(
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
