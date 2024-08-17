package nobleidl.compiler.api
@_root_.esexpr.constructor("noble-idl-generation-result")
final case class NobleIdlGenerationResult(
  @_root_.esexpr.keyword("generated-files")
  generatedFiles: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object NobleIdlGenerationResult {
}
