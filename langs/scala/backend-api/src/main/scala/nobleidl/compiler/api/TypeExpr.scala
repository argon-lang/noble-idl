package nobleidl.compiler.api
enum TypeExpr derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("defined-type")
  case DefinedType(
    name: _root_.nobleidl.compiler.api.QualifiedName,
    @_root_.esexpr.vararg
    args: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.TypeExpr],
  )
  @_root_.esexpr.constructor("type-parameter")
  case TypeParameter(
    name: _root_.nobleidl.core.String,
    @_root_.esexpr.keyword("owner")
    owner: _root_.nobleidl.compiler.api.TypeParameterOwner,
  )
}
object TypeExpr {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.TypeExpr): _root_.dev.argon.nobleidl.compiler.api.TypeExpr = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.TypeExpr.DefinedType =>
            new _root_.dev.argon.nobleidl.compiler.api.TypeExpr.DefinedType(
              _root_.nobleidl.compiler.api.QualifiedName.javaAdapter().toJava(s_value.name),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).toJava(s_value.args),
            )
          case s_value: _root_.nobleidl.compiler.api.TypeExpr.TypeParameter =>
            new _root_.dev.argon.nobleidl.compiler.api.TypeExpr.TypeParameter(
              _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
              _root_.nobleidl.compiler.api.TypeParameterOwner.javaAdapter().toJava(s_value.owner),
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.TypeExpr): _root_.nobleidl.compiler.api.TypeExpr = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.TypeExpr.DefinedType =>
            new _root_.nobleidl.compiler.api.TypeExpr.DefinedType(
              _root_.nobleidl.compiler.api.QualifiedName.javaAdapter().fromJava(j_value.name().nn),
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.TypeExpr, _root_.dev.argon.nobleidl.compiler.api.TypeExpr](_root_.nobleidl.compiler.api.TypeExpr.javaAdapter()).fromJava(j_value.args().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.TypeExpr.TypeParameter =>
            new _root_.nobleidl.compiler.api.TypeExpr.TypeParameter(
              _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
              _root_.nobleidl.compiler.api.TypeParameterOwner.javaAdapter().fromJava(j_value.owner().nn),
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
