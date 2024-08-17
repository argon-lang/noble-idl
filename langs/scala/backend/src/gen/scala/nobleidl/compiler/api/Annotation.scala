package nobleidl.compiler.api
@_root_.esexpr.constructor("annotation")
final case class Annotation(
  scope: _root_.nobleidl.core.String,
  value: _root_.nobleidl.core.Esexpr,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object Annotation {
}
