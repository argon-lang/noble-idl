package nobleidl.compiler.api
@_root_.esexpr.constructor("interface-definition")
final case class InterfaceDefinition(
  @_root_.esexpr.vararg
  methods: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.InterfaceMethod],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object InterfaceDefinition {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.InterfaceDefinition, _root_.dev.argon.nobleidl.compiler.api.InterfaceDefinition] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.InterfaceDefinition, _root_.dev.argon.nobleidl.compiler.api.InterfaceDefinition] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.InterfaceDefinition): _root_.dev.argon.nobleidl.compiler.api.InterfaceDefinition = {
        new _root_.dev.argon.nobleidl.compiler.api.InterfaceDefinition(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.InterfaceMethod, _root_.dev.argon.nobleidl.compiler.api.InterfaceMethod](_root_.nobleidl.compiler.api.InterfaceMethod.javaAdapter()).toJava(s_value.methods),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.InterfaceDefinition): _root_.nobleidl.compiler.api.InterfaceDefinition = {
        _root_.nobleidl.compiler.api.InterfaceDefinition(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.InterfaceMethod, _root_.dev.argon.nobleidl.compiler.api.InterfaceMethod](_root_.nobleidl.compiler.api.InterfaceMethod.javaAdapter()).fromJava(j_value.methods().nn),
        )
      }
    }
}
