package nobleidl.compiler.api
@_root_.esexpr.constructor("annotation")
final case class Annotation(
  scope: _root_.nobleidl.core.String,
  value: _root_.nobleidl.core.Esexpr,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object Annotation {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.Annotation): _root_.dev.argon.nobleidl.compiler.api.Annotation = {
        new _root_.dev.argon.nobleidl.compiler.api.Annotation(
          _root_.nobleidl.core.String.javaAdapter().toJava(s_value.scope),
          _root_.nobleidl.core.Esexpr.javaAdapter().toJava(s_value.value),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.Annotation): _root_.nobleidl.compiler.api.Annotation = {
        _root_.nobleidl.compiler.api.Annotation(
          _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.scope().nn),
          _root_.nobleidl.core.Esexpr.javaAdapter().fromJava(j_value.value().nn),
        )
      }
    }
}
