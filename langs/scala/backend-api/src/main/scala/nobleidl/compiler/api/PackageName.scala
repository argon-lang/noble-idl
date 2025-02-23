package nobleidl.compiler.api
@_root_.esexpr.constructor("package-name")
final case class PackageName(
  @_root_.esexpr.vararg
  parts: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object PackageName {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.PackageName, _root_.dev.argon.nobleidl.compiler.api.PackageName] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.PackageName, _root_.dev.argon.nobleidl.compiler.api.PackageName] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.PackageName): _root_.dev.argon.nobleidl.compiler.api.PackageName = {
        new _root_.dev.argon.nobleidl.compiler.api.PackageName(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).toJava(s_value.parts),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.PackageName): _root_.nobleidl.compiler.api.PackageName = {
        _root_.nobleidl.compiler.api.PackageName(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).fromJava(j_value.parts().nn),
        )
      }
    }
}
