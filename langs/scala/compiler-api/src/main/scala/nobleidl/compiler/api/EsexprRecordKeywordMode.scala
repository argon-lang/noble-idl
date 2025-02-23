package nobleidl.compiler.api
enum EsexprRecordKeywordMode derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("required")
  case Required(
  )
  @_root_.esexpr.constructor("optional")
  case Optional(
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
  )
  @_root_.esexpr.constructor("default-value")
  case DefaultValue(
    value: _root_.nobleidl.compiler.api.EsexprDecodedValue,
  )
}
object EsexprRecordKeywordMode {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprRecordKeywordMode, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprRecordKeywordMode, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprRecordKeywordMode): _root_.dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.EsexprRecordKeywordMode.Required =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode.Required(
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprRecordKeywordMode.Optional =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode.Optional(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.elementType),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprRecordKeywordMode.DefaultValue =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode.DefaultValue(
              _root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter().toJava(s_value.value),
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode): _root_.nobleidl.compiler.api.EsexprRecordKeywordMode = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode.Required =>
            new _root_.nobleidl.compiler.api.EsexprRecordKeywordMode.Required(
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode.Optional =>
            new _root_.nobleidl.compiler.api.EsexprRecordKeywordMode.Optional(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.elementType().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordKeywordMode.DefaultValue =>
            new _root_.nobleidl.compiler.api.EsexprRecordKeywordMode.DefaultValue(
              _root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter().fromJava(j_value.value().nn),
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
