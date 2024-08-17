package nobleidl.compiler.api
@_root_.esexpr.constructor("noble-idl-generation-request")
final case class NobleIdlGenerationRequest[L](
  @_root_.esexpr.keyword("language-options")
  languageOptions: L,
  @_root_.esexpr.keyword("model")
  model: _root_.nobleidl.compiler.api.NobleIdlModel,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object NobleIdlGenerationRequest {
}
