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
}
