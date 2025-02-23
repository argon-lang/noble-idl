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
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.TypeParameter, _root_.dev.argon.nobleidl.compiler.api.TypeParameter] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.TypeParameter, _root_.dev.argon.nobleidl.compiler.api.TypeParameter] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.TypeParameter): _root_.dev.argon.nobleidl.compiler.api.TypeParameter = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.TypeParameter.Type =>
            new _root_.dev.argon.nobleidl.compiler.api.TypeParameter.Type(
              _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.TypeParameterTypeConstraint, _root_.dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint](_root_.nobleidl.compiler.api.TypeParameterTypeConstraint.javaAdapter()).toJava(s_value.constraints),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).toJava(s_value.annotations),
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.TypeParameter): _root_.nobleidl.compiler.api.TypeParameter = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.TypeParameter.Type =>
            new _root_.nobleidl.compiler.api.TypeParameter.Type(
              _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.TypeParameterTypeConstraint, _root_.dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint](_root_.nobleidl.compiler.api.TypeParameterTypeConstraint.javaAdapter()).fromJava(j_value.constraints().nn),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).fromJava(j_value.annotations().nn),
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
