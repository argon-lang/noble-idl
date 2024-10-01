package nobleidl.compiler.api
@_root_.esexpr.constructor("interface-method-parameter")
final case class InterfaceMethodParameter(
  name: _root_.nobleidl.core.String,
  parameterType: _root_.nobleidl.compiler.api.TypeExpr,
  @_root_.esexpr.keyword("annotations")
  annotations: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.Annotation],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object InterfaceMethodParameter {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.InterfaceMethodParameter, _root_.dev.argon.nobleidl.compiler.api.InterfaceMethodParameter] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.InterfaceMethodParameter, _root_.dev.argon.nobleidl.compiler.api.InterfaceMethodParameter] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.InterfaceMethodParameter): _root_.dev.argon.nobleidl.compiler.api.InterfaceMethodParameter = {
        new _root_.dev.argon.nobleidl.compiler.api.InterfaceMethodParameter(
          _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
          _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.parameterType),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).toJava(s_value.annotations),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.InterfaceMethodParameter): _root_.nobleidl.compiler.api.InterfaceMethodParameter = {
        _root_.nobleidl.compiler.api.InterfaceMethodParameter(
          _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
          _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.parameterType().nn),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).fromJava(j_value.annotations().nn),
        )
      }
    }
}
