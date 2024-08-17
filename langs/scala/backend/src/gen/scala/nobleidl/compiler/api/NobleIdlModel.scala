package nobleidl.compiler.api
@_root_.esexpr.constructor("noble-idl-model")
final case class NobleIdlModel(
  @_root_.esexpr.keyword("definitions")
  definitions: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.DefinitionInfo],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object NobleIdlModel {
}
