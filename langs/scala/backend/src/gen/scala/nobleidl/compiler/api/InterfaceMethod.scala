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
}
