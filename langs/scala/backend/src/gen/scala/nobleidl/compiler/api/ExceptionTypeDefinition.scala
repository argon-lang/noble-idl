package nobleidl.compiler.api
@_root_.esexpr.constructor("exception-type-definition")
final case class ExceptionTypeDefinition(
  information: _root_.nobleidl.compiler.api.TypeExpr,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object ExceptionTypeDefinition {
}
