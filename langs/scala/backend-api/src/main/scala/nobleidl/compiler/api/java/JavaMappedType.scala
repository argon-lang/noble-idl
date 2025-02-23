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
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.java.JavaMappedType, _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.java.JavaMappedType, _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.java.JavaMappedType): _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.java.JavaMappedType.TypeName =>
            new _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType.TypeName(
              _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
            )
          case s_value: _root_.nobleidl.compiler.api.java.JavaMappedType.Apply =>
            new _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType.Apply(
              _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.java.JavaMappedType, _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType](_root_.nobleidl.compiler.api.java.JavaMappedType.javaAdapter()).toJava(s_value.args),
            )
          case s_value: _root_.nobleidl.compiler.api.java.JavaMappedType.Annotated =>
            new _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType.Annotated(
              _root_.nobleidl.compiler.api.java.JavaMappedType.javaAdapter().toJava(s_value.t),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).toJava(s_value.annotations),
            )
          case s_value: _root_.nobleidl.compiler.api.java.JavaMappedType.TypeParameter =>
            new _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType.TypeParameter(
              _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
            )
          case s_value: _root_.nobleidl.compiler.api.java.JavaMappedType.Array =>
            new _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType.Array(
              _root_.nobleidl.compiler.api.java.JavaMappedType.javaAdapter().toJava(s_value.elementType),
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType): _root_.nobleidl.compiler.api.java.JavaMappedType = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType.TypeName =>
            new _root_.nobleidl.compiler.api.java.JavaMappedType.TypeName(
              _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType.Apply =>
            new _root_.nobleidl.compiler.api.java.JavaMappedType.Apply(
              _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.java.JavaMappedType, _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType](_root_.nobleidl.compiler.api.java.JavaMappedType.javaAdapter()).fromJava(j_value.args().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType.Annotated =>
            new _root_.nobleidl.compiler.api.java.JavaMappedType.Annotated(
              _root_.nobleidl.compiler.api.java.JavaMappedType.javaAdapter().fromJava(j_value.t().nn),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).fromJava(j_value.annotations().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType.TypeParameter =>
            new _root_.nobleidl.compiler.api.java.JavaMappedType.TypeParameter(
              _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.java.JavaMappedType.Array =>
            new _root_.nobleidl.compiler.api.java.JavaMappedType.Array(
              _root_.nobleidl.compiler.api.java.JavaMappedType.javaAdapter().fromJava(j_value.elementType().nn),
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
