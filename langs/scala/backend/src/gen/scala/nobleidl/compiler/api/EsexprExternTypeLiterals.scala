package nobleidl.compiler.api
@_root_.esexpr.constructor("literals")
final case class EsexprExternTypeLiterals(
  @_root_.esexpr.keyword("allow-bool")
  allowBool: _root_.nobleidl.core.Bool = _root_.nobleidl.core.Bool.fromBoolean(false),
  @_root_.esexpr.keyword("allow-int")
  allowInt: _root_.nobleidl.core.Bool = _root_.nobleidl.core.Bool.fromBoolean(false),
  @_root_.esexpr.keyword("min-int")
  @_root_.esexpr.optional
  minInt: _root_.nobleidl.core.OptionalField[_root_.nobleidl.core.Int],
  @_root_.esexpr.keyword("max-int")
  @_root_.esexpr.optional
  maxInt: _root_.nobleidl.core.OptionalField[_root_.nobleidl.core.Int],
  @_root_.esexpr.keyword("allow-str")
  allowStr: _root_.nobleidl.core.Bool = _root_.nobleidl.core.Bool.fromBoolean(false),
  @_root_.esexpr.keyword("allow-binary")
  allowBinary: _root_.nobleidl.core.Bool = _root_.nobleidl.core.Bool.fromBoolean(false),
  @_root_.esexpr.keyword("allow-float32")
  allowFloat32: _root_.nobleidl.core.Bool = _root_.nobleidl.core.Bool.fromBoolean(false),
  @_root_.esexpr.keyword("allow-float64")
  allowFloat64: _root_.nobleidl.core.Bool = _root_.nobleidl.core.Bool.fromBoolean(false),
  @_root_.esexpr.keyword("allow-null")
  allowNull: _root_.nobleidl.core.Bool = _root_.nobleidl.core.Bool.fromBoolean(false),
  @_root_.esexpr.keyword("build-literal-from")
  @_root_.esexpr.optional
  buildLiteralFrom: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.TypeExpr],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EsexprExternTypeLiterals {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprExternTypeLiterals, _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeLiterals] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprExternTypeLiterals, _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeLiterals] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprExternTypeLiterals): _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeLiterals = {
        new _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeLiterals(
          _root_.nobleidl.core.Bool.javaAdapter().toJava(s_value.allowBool),
          _root_.nobleidl.core.Bool.javaAdapter().toJava(s_value.allowInt),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Int, _root_.java.math.BigInteger](_root_.nobleidl.core.Int.javaAdapter()).toJava(s_value.minInt),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Int, _root_.java.math.BigInteger](_root_.nobleidl.core.Int.javaAdapter()).toJava(s_value.maxInt),
          _root_.nobleidl.core.Bool.javaAdapter().toJava(s_value.allowStr),
          _root_.nobleidl.core.Bool.javaAdapter().toJava(s_value.allowBinary),
          _root_.nobleidl.core.Bool.javaAdapter().toJava(s_value.allowFloat32),
          _root_.nobleidl.core.Bool.javaAdapter().toJava(s_value.allowFloat64),
          _root_.nobleidl.core.Bool.javaAdapter().toJava(s_value.allowNull),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).toJava(s_value.buildLiteralFrom),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeLiterals): _root_.nobleidl.compiler.api.EsexprExternTypeLiterals = {
        _root_.nobleidl.compiler.api.EsexprExternTypeLiterals(
          _root_.nobleidl.core.Bool.javaAdapter().fromJava(j_value.allowBool().nn),
          _root_.nobleidl.core.Bool.javaAdapter().fromJava(j_value.allowInt().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Int, _root_.java.math.BigInteger](_root_.nobleidl.core.Int.javaAdapter()).fromJava(j_value.minInt().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Int, _root_.java.math.BigInteger](_root_.nobleidl.core.Int.javaAdapter()).fromJava(j_value.maxInt().nn),
          _root_.nobleidl.core.Bool.javaAdapter().fromJava(j_value.allowStr().nn),
          _root_.nobleidl.core.Bool.javaAdapter().fromJava(j_value.allowBinary().nn),
          _root_.nobleidl.core.Bool.javaAdapter().fromJava(j_value.allowFloat32().nn),
          _root_.nobleidl.core.Bool.javaAdapter().fromJava(j_value.allowFloat64().nn),
          _root_.nobleidl.core.Bool.javaAdapter().fromJava(j_value.allowNull().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).fromJava(j_value.buildLiteralFrom().nn),
        )
      }
    }
}
