package nobleidl.compiler.api
enum EsexprDecodedValue derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("record")
  case Record(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    @_root_.esexpr.vararg
    fields: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.EsexprDecodedFieldValue],
  )
  @_root_.esexpr.constructor("enum")
  case Enum(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    caseName: _root_.nobleidl.core.String,
    @_root_.esexpr.vararg
    fields: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.EsexprDecodedFieldValue],
  )
  @_root_.esexpr.constructor("simple-enum")
  case SimpleEnum(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    caseName: _root_.nobleidl.core.String,
  )
  @_root_.esexpr.constructor("optional")
  case Optional(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
    @_root_.esexpr.optional
    value: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.EsexprDecodedValue],
  )
  @_root_.esexpr.constructor("vararg")
  case Vararg(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
    @_root_.esexpr.vararg
    values: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.EsexprDecodedValue],
  )
  @_root_.esexpr.constructor("dict")
  case Dict(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    elementType: _root_.nobleidl.compiler.api.TypeExpr,
    @_root_.esexpr.dict
    values: _root_.nobleidl.core.Dict[_root_.nobleidl.compiler.api.EsexprDecodedValue],
  )
  @_root_.esexpr.constructor("build-from")
  case BuildFrom(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    fromType: _root_.nobleidl.compiler.api.TypeExpr,
    fromValue: _root_.nobleidl.compiler.api.EsexprDecodedValue,
  )
  @_root_.esexpr.constructor("from-bool")
  case FromBool(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    b: _root_.nobleidl.core.Bool,
  )
  @_root_.esexpr.constructor("from-int")
  case FromInt(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    i: _root_.nobleidl.core.Int,
    @_root_.esexpr.keyword("min-int")
    @_root_.esexpr.optional
    minInt: _root_.nobleidl.core.OptionalField[_root_.nobleidl.core.Int],
    @_root_.esexpr.keyword("max-int")
    @_root_.esexpr.optional
    maxInt: _root_.nobleidl.core.OptionalField[_root_.nobleidl.core.Int],
  )
  @_root_.esexpr.constructor("from-str")
  case FromStr(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    s: _root_.nobleidl.core.String,
  )
  @_root_.esexpr.constructor("from-binary")
  case FromBinary(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    b: _root_.nobleidl.core.Binary,
  )
  @_root_.esexpr.constructor("from-float32")
  case FromFloat32(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    f: _root_.nobleidl.core.F32,
  )
  @_root_.esexpr.constructor("from-float64")
  case FromFloat64(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    f: _root_.nobleidl.core.F64,
  )
  @_root_.esexpr.constructor("from-null")
  case FromNull(
    t: _root_.nobleidl.compiler.api.TypeExpr,
    @_root_.esexpr.optional
    level: _root_.nobleidl.core.OptionalField[_root_.nobleidl.core.Nat],
    @_root_.esexpr.keyword("max-level")
    @_root_.esexpr.optional
    maxLevel: _root_.nobleidl.core.OptionalField[_root_.nobleidl.core.Nat],
  )
}
object EsexprDecodedValue {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue): _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.Record =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.Record(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedFieldValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue](_root_.nobleidl.compiler.api.EsexprDecodedFieldValue.javaAdapter()).toJava(s_value.fields),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.Enum =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.Enum(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.String.javaAdapter().toJava(s_value.caseName),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedFieldValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue](_root_.nobleidl.compiler.api.EsexprDecodedFieldValue.javaAdapter()).toJava(s_value.fields),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.SimpleEnum =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.SimpleEnum(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.String.javaAdapter().toJava(s_value.caseName),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.Optional =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.Optional(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.elementType),
              _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue](_root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter()).toJava(s_value.value),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.Vararg =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.Vararg(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.elementType),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue](_root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter()).toJava(s_value.values),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.Dict =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.Dict(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.elementType),
              _root_.nobleidl.core.Dict.javaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue](_root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter()).toJava(s_value.values),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.BuildFrom =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.BuildFrom(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.fromType),
              _root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter().toJava(s_value.fromValue),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.FromBool =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromBool(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.Bool.javaAdapter().toJava(s_value.b),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.FromInt =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromInt(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.Int.javaAdapter().toJava(s_value.i),
              _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Int, _root_.java.math.BigInteger](_root_.nobleidl.core.Int.javaAdapter()).toJava(s_value.minInt),
              _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Int, _root_.java.math.BigInteger](_root_.nobleidl.core.Int.javaAdapter()).toJava(s_value.maxInt),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.FromStr =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromStr(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.String.javaAdapter().toJava(s_value.s),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.FromBinary =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromBinary(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.Binary.javaAdapter().toJava(s_value.b),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.FromFloat32 =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromFloat32(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.F32.javaAdapter().toJava(s_value.f),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.FromFloat64 =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromFloat64(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.F64.javaAdapter().toJava(s_value.f),
            )
          case s_value: _root_.nobleidl.compiler.api.EsexprDecodedValue.FromNull =>
            new _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromNull(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Nat, _root_.java.math.BigInteger](_root_.nobleidl.core.Nat.javaAdapter()).toJava(s_value.level),
              _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Nat, _root_.java.math.BigInteger](_root_.nobleidl.core.Nat.javaAdapter()).toJava(s_value.maxLevel),
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue): _root_.nobleidl.compiler.api.EsexprDecodedValue = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.Record =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.Record(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedFieldValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue](_root_.nobleidl.compiler.api.EsexprDecodedFieldValue.javaAdapter()).fromJava(j_value.fields().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.Enum =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.Enum(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.caseName().nn),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedFieldValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedFieldValue](_root_.nobleidl.compiler.api.EsexprDecodedFieldValue.javaAdapter()).fromJava(j_value.fields().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.SimpleEnum =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.SimpleEnum(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.caseName().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.Optional =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.Optional(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.elementType().nn),
              _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue](_root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter()).fromJava(j_value.value().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.Vararg =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.Vararg(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.elementType().nn),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue](_root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter()).fromJava(j_value.values().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.Dict =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.Dict(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.elementType().nn),
              _root_.nobleidl.core.Dict.javaAdapter[_root_.nobleidl.compiler.api.EsexprDecodedValue, _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue](_root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter()).fromJava(j_value.values().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.BuildFrom =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.BuildFrom(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.fromType().nn),
              _root_.nobleidl.compiler.api.EsexprDecodedValue.javaAdapter().fromJava(j_value.fromValue().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromBool =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.FromBool(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.Bool.javaAdapter().fromJava(j_value.b().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromInt =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.FromInt(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.Int.javaAdapter().fromJava(j_value.i().nn),
              _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Int, _root_.java.math.BigInteger](_root_.nobleidl.core.Int.javaAdapter()).fromJava(j_value.minInt().nn),
              _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Int, _root_.java.math.BigInteger](_root_.nobleidl.core.Int.javaAdapter()).fromJava(j_value.maxInt().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromStr =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.FromStr(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.s().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromBinary =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.FromBinary(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.Binary.javaAdapter().fromJava(j_value.b().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromFloat32 =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.FromFloat32(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.F32.javaAdapter().fromJava(j_value.f().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromFloat64 =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.FromFloat64(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.F64.javaAdapter().fromJava(j_value.f().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.EsexprDecodedValue.FromNull =>
            new _root_.nobleidl.compiler.api.EsexprDecodedValue.FromNull(
              _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Nat, _root_.java.math.BigInteger](_root_.nobleidl.core.Nat.javaAdapter()).fromJava(j_value.level().nn),
              _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.core.Nat, _root_.java.math.BigInteger](_root_.nobleidl.core.Nat.javaAdapter()).fromJava(j_value.maxLevel().nn),
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
