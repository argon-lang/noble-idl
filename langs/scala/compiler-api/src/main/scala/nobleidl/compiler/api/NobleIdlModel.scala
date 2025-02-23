package nobleidl.compiler.api
@_root_.esexpr.constructor("noble-idl-model")
final case class NobleIdlModel(
  @_root_.esexpr.keyword("definitions")
  definitions: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.DefinitionInfo],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object NobleIdlModel {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.NobleIdlModel, _root_.dev.argon.nobleidl.compiler.api.NobleIdlModel] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.NobleIdlModel, _root_.dev.argon.nobleidl.compiler.api.NobleIdlModel] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.NobleIdlModel): _root_.dev.argon.nobleidl.compiler.api.NobleIdlModel = {
        new _root_.dev.argon.nobleidl.compiler.api.NobleIdlModel(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.DefinitionInfo, _root_.dev.argon.nobleidl.compiler.api.DefinitionInfo](_root_.nobleidl.compiler.api.DefinitionInfo.javaAdapter()).toJava(s_value.definitions),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.NobleIdlModel): _root_.nobleidl.compiler.api.NobleIdlModel = {
        _root_.nobleidl.compiler.api.NobleIdlModel(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.DefinitionInfo, _root_.dev.argon.nobleidl.compiler.api.DefinitionInfo](_root_.nobleidl.compiler.api.DefinitionInfo.javaAdapter()).fromJava(j_value.definitions().nn),
        )
      }
    }
}
