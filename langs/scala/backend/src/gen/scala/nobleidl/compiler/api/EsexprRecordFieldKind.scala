package nobleidl.compiler.api
enum EsexprRecordFieldKind derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("positional")
  case Positional(
    mode: _root_.nobleidl.compiler.api.EsexprRecordPositionalMode,
  )
  @_root_.esexpr.constructor("keyword")
  case Keyword(
    name: _root_.nobleidl.core.String,
    mode: _root_.nobleidl.compiler.api.EsexprRecordKeywordMode,
  )
  @_root_.esexpr.constructor("dict")
  case Dict(
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
  )
  @_root_.esexpr.constructor("vararg")
  case Vararg(
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
  )
}
object EsexprRecordFieldKind {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprRecordFieldKind, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprRecordFieldKind, _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprRecordFieldKind): _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.EsexprRecordFieldKind.Positional =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind.Positional(
              _root_.nobleidl.compiler.api.EsexprRecordPositionalMode.javaAdapter().toJava(s_value.mode),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprRecordFieldKind.Keyword =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind.Keyword(
              _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
              _root_.nobleidl.compiler.api.EsexprRecordKeywordMode.javaAdapter().toJava(s_value.mode),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprRecordFieldKind.Dict =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind.Dict(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.elementType),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprRecordFieldKind.Vararg =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind.Vararg(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.elementType),
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind): _root_.nobleidl.compiler.api.EsexprRecordFieldKind = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind.Positional =>
            new _root_.nobleidl.compiler.api.EsexprRecordFieldKind.Positional(
              _root_.nobleidl.compiler.api.EsexprRecordPositionalMode.javaAdapter().fromJava(j_value.mode().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind.Keyword =>
            new _root_.nobleidl.compiler.api.EsexprRecordFieldKind.Keyword(
              _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
              _root_.nobleidl.compiler.api.EsexprRecordKeywordMode.javaAdapter().fromJava(j_value.mode().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind.Dict =>
            new _root_.nobleidl.compiler.api.EsexprRecordFieldKind.Dict(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.elementType().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprRecordFieldKind.Vararg =>
            new _root_.nobleidl.compiler.api.EsexprRecordFieldKind.Vararg(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.elementType().nn),
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
