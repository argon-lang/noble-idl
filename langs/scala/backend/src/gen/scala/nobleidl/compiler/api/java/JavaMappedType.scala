package nobleidl.compiler.api.java
enum JavaMappedType derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.inlineValue
  case TypeName(
    name: _root_.nobleidl.core.String,
  )
  @_root_.esexpr.constructor("apply")
  case Apply(
    name: _root_.nobleidl.core.String,
    @_root_.esexpr.vararg
    args: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.java.JavaMappedType],
  )
  @_root_.esexpr.constructor("annotated")
  case Annotated(
    t: _root_.nobleidl.compiler.api.java.JavaMappedType,
    @_root_.esexpr.vararg
    annotations: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
  )
  @_root_.esexpr.constructor("type-parameter")
  case TypeParameter(
    name: _root_.nobleidl.core.String,
  )
  @_root_.esexpr.constructor("array")
  case Array(
    elementType: _root_.nobleidl.compiler.api.java.JavaMappedType,
  )
}
object JavaMappedType {
}
