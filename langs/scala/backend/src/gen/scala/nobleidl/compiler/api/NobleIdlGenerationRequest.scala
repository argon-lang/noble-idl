package nobleidl.compiler.api
@_root_.esexpr.constructor("noble-idl-generation-request")
final case class NobleIdlGenerationRequest[L](
  @_root_.esexpr.keyword("language-options")
  languageOptions: L,
  @_root_.esexpr.keyword("model")
  model: _root_.nobleidl.compiler.api.NobleIdlModel,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object NobleIdlGenerationRequest {
  def javaAdapter[SL, JL](lAdapter: _root_.nobleidl.core.JavaAdapter[SL, JL]): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.NobleIdlGenerationRequest[SL], _root_.dev.argon.nobleidl.compiler.api.NobleIdlGenerationRequest[JL]] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.NobleIdlGenerationRequest[SL], _root_.dev.argon.nobleidl.compiler.api.NobleIdlGenerationRequest[JL]] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.NobleIdlGenerationRequest[SL]): _root_.dev.argon.nobleidl.compiler.api.NobleIdlGenerationRequest[JL] = {
        new _root_.dev.argon.nobleidl.compiler.api.NobleIdlGenerationRequest[JL](
          lAdapter.toJava(s_value.languageOptions),
          _root_.nobleidl.compiler.api.NobleIdlModel.javaAdapter().toJava(s_value.model),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.NobleIdlGenerationRequest[JL]): _root_.nobleidl.compiler.api.NobleIdlGenerationRequest[SL] = {
        _root_.nobleidl.compiler.api.NobleIdlGenerationRequest[SL](
          lAdapter.fromJava(j_value.languageOptions().nn),
          _root_.nobleidl.compiler.api.NobleIdlModel.javaAdapter().fromJava(j_value.model().nn),
        )
      }
    }
}
