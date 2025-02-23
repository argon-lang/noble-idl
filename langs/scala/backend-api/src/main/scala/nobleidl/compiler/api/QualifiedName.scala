package nobleidl.compiler.api
@_root_.esexpr.constructor("qualified-name")
final case class QualifiedName(
  `package`: _root_.nobleidl.compiler.api.PackageName,
  name: _root_.nobleidl.core.String,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object QualifiedName {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.QualifiedName, _root_.dev.argon.nobleidl.compiler.api.QualifiedName] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.QualifiedName, _root_.dev.argon.nobleidl.compiler.api.QualifiedName] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.QualifiedName): _root_.dev.argon.nobleidl.compiler.api.QualifiedName = {
        new _root_.dev.argon.nobleidl.compiler.api.QualifiedName(
          _root_.nobleidl.compiler.api.PackageName.javaAdapter().toJava(s_value.`package`),
          _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.QualifiedName): _root_.nobleidl.compiler.api.QualifiedName = {
        _root_.nobleidl.compiler.api.QualifiedName(
          _root_.nobleidl.compiler.api.PackageName.javaAdapter().fromJava(j_value._package().nn),
          _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
        )
      }
    }
}
