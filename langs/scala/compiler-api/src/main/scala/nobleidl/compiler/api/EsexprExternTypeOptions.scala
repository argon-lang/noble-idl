package nobleidl.compiler.api
@_root_.esexpr.constructor("extern-type-options")
final case class EsexprExternTypeOptions(
  @_root_.esexpr.keyword("allow-value")
  allowValue: _root_.nobleidl.core.Bool = _root_.nobleidl.core.Bool.fromBoolean(false),
  @_root_.esexpr.keyword("allow-optional")
  @_root_.esexpr.optional
  allowOptional: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.TypeExpr],
  @_root_.esexpr.keyword("allow-vararg")
  @_root_.esexpr.optional
  allowVararg: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.TypeExpr],
  @_root_.esexpr.keyword("allow-dict")
  @_root_.esexpr.optional
  allowDict: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.TypeExpr],
  @_root_.esexpr.keyword("literals")
  literals: _root_.nobleidl.compiler.api.EsexprExternTypeLiterals,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprExternTypeOptions {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprExternTypeOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeOptions] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprExternTypeOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeOptions] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprExternTypeOptions): _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeOptions = {
        new _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeOptions(
          _root_.nobleidl.core.Bool.javaAdapter().toJava(s_value.allowValue),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).toJava(s_value.allowOptional),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).toJava(s_value.allowVararg),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).toJava(s_value.allowDict),
          _root_.nobleidl.compiler.api.EsexprExternTypeLiterals.javaAdapter().toJava(s_value.literals),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeOptions): _root_.nobleidl.compiler.api.EsexprExternTypeOptions = {
        _root_.nobleidl.compiler.api.EsexprExternTypeOptions(
          _root_.nobleidl.core.Bool.javaAdapter().fromJava(j_value.allowValue().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).fromJava(j_value.allowOptional().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).fromJava(j_value.allowVararg().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).fromJava(j_value.allowDict().nn),
          _root_.nobleidl.compiler.api.EsexprExternTypeLiterals.javaAdapter().fromJava(j_value.literals().nn),
        )
      }
    }
}
