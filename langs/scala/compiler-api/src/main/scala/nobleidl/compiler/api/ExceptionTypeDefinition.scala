package nobleidl.compiler.api
@_root_.esexpr.constructor("exception-type-definition")
final case class ExceptionTypeDefinition(
  information: _root_.nobleidl.compiler.api.TypeExpr,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object ExceptionTypeDefinition {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.ExceptionTypeDefinition, _root_.dev.argon.nobleidl.compiler.api.ExceptionTypeDefinition] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.ExceptionTypeDefinition, _root_.dev.argon.nobleidl.compiler.api.ExceptionTypeDefinition] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.ExceptionTypeDefinition): _root_.dev.argon.nobleidl.compiler.api.ExceptionTypeDefinition = {
        new _root_.dev.argon.nobleidl.compiler.api.ExceptionTypeDefinition(
          _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().toJava(s_value.information),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.ExceptionTypeDefinition): _root_.nobleidl.compiler.api.ExceptionTypeDefinition = {
        _root_.nobleidl.compiler.api.ExceptionTypeDefinition(
          _root_.nobleidl.compiler.api.TypeExpr.javaAdapter().fromJava(j_value.information().nn),
        )
      }
    }
}
