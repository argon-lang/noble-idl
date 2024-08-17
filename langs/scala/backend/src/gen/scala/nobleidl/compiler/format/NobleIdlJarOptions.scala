package nobleidl.compiler.format
@_root_.esexpr.constructor("noble-idl-options")
final case class NobleIdlJarOptions(
  @_root_.esexpr.keyword("idl-files")
  idlFiles: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
  @_root_.esexpr.keyword("backends")
  backends: _root_.nobleidl.compiler.format.BackendMapping,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object NobleIdlJarOptions {
}
