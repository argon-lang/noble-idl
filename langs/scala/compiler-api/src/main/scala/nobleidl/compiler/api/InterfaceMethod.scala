package nobleidl.compiler.api
@_root_.esexpr.constructor("interface-method")
final case class InterfaceMethod(
  @_root_.esexpr.keyword("name")
  name: _root_.nobleidl.core.String,
  @_root_.esexpr.keyword("type-parameters")
  typeParameters: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.TypeParameter],
  @_root_.esexpr.keyword("parameters")
  parameters: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.InterfaceMethodParameter],
  @_root_.esexpr.keyword("return-type")
  returnType: _root_.nobleidl.compiler.api.TypeExpr,
  @_root_.esexpr.keyword("throws")
  @_root_.esexpr.optional
  throws: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.TypeExpr],
  @_root_.esexpr.keyword("annotations")
  annotations: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.Annotation],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object InterfaceMethod {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.InterfaceMethod, _root_.dev.argon.nobleidl.compiler.api.InterfaceMethod] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.InterfaceMethod, _root_.dev.argon.nobleidl.compiler.api.InterfaceMethod] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.InterfaceMethod): _root_.dev.argon.nobleidl.compiler.api.InterfaceMethod = {
        new _root_.dev.argon.nobleidl.compiler.api.InterfaceMethod(
          _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.TypeParameter, _root_.dev.argon.nobleidl.compiler.api.TypeParameter](_root_.nobleidl.compiler.api.TypeParameter.javaAdapter()).toJava(s_value.typeParameters),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.InterfaceMethodParameter, _root_.dev.argon.nobleidl.compiler.api.InterfaceMethodParameter](_root_.nobleidl.compiler.api.InterfaceMethodParameter.javaAdapter()).toJava(s_value.parameters),
          _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.returnType),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).toJava(s_value.throws),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).toJava(s_value.annotations),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.InterfaceMethod): _root_.nobleidl.compiler.api.InterfaceMethod = {
        _root_.nobleidl.compiler.api.InterfaceMethod(
          _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.TypeParameter, _root_.dev.argon.nobleidl.compiler.api.TypeParameter](_root_.nobleidl.compiler.api.TypeParameter.javaAdapter()).fromJava(j_value.typeParameters().nn),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.InterfaceMethodParameter, _root_.dev.argon.nobleidl.compiler.api.InterfaceMethodParameter](_root_.nobleidl.compiler.api.InterfaceMethodParameter.javaAdapter()).fromJava(j_value.parameters().nn),
          _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.returnType().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).fromJava(j_value._throws().nn),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).fromJava(j_value.annotations().nn),
        )
      }
    }
}
