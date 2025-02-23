package nobleidl.compiler.api
enum TypeParameterTypeConstraint derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("exception")
  case Exception(
  )
}
object TypeParameterTypeConstraint {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.TypeParameterTypeConstraint, _root_.dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.TypeParameterTypeConstraint, _root_.dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.TypeParameterTypeConstraint): _root_.dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.TypeParameterTypeConstraint.Exception =>
            new _root_.dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint.Exception(
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint): _root_.nobleidl.compiler.api.TypeParameterTypeConstraint = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.TypeParameterTypeConstraint.Exception =>
            new _root_.nobleidl.compiler.api.TypeParameterTypeConstraint.Exception(
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
