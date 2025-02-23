package nobleidl.compiler.api
enum EsexprRecordPositionalMode derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("required")
  case Required(
  )
  @_root_.esexpr.constructor("optional")
  case Optional(
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
  )
}
object EsexprRecordPositionalMode {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprRecordPositionalMode, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprRecordPositionalMode, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprRecordPositionalMode): _root_.dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.EsexprRecordPositionalMode.Required =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode.Required(
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprRecordPositionalMode.Optional =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode.Optional(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.elementType),
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode): _root_.nobleidl.compiler.api.EsexprRecordPositionalMode = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode.Required =>
            new _root_.nobleidl.compiler.api.EsexprRecordPositionalMode.Required(
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordPositionalMode.Optional =>
            new _root_.nobleidl.compiler.api.EsexprRecordPositionalMode.Optional(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.elementType().nn),
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
