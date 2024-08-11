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
  )
}
