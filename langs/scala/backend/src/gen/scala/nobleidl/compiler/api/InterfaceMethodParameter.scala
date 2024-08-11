package nobleidl.compiler.api
@_root_.esexpr.constructor("interface-method-parameter")
final case class InterfaceMethodParameter(
  name: _root_.nobleidl.core.String,
  parameterType: _root_.nobleidl.compiler.api.TypeExpr,
  @_root_.esexpr.keyword("annotations")
  annotations: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.Annotation],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
