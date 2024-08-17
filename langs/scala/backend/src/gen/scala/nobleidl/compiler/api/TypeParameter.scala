package nobleidl.compiler.api
enum TypeParameter derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("type")
  case Type(
    name: _root_.nobleidl.core.String,
    @_root_.esexpr.keyword("constraints")
    constraints: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.TypeParameterTypeConstraint] = _root_.nobleidl.core.List.buildFrom[_root_.nobleidl.compiler.api.TypeParameterTypeConstraint](_root_.nobleidl.core.ListRepr[_root_.nobleidl.compiler.api.TypeParameterTypeConstraint](_root_.nobleidl.core.List.fromSeq[_root_.nobleidl.compiler.api.TypeParameterTypeConstraint](_root_.scala.collection.immutable.Seq[_root_.nobleidl.compiler.api.TypeParameterTypeConstraint]()))),
    @_root_.esexpr.keyword("annotations")
    annotations: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.Annotation],
  )
}
object TypeParameter {
}
