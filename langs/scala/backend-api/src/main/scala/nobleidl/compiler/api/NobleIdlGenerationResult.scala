package nobleidl.compiler.api
@_root_.esexpr.constructor("noble-idl-generation-result")
final case class NobleIdlGenerationResult(
  @_root_.esexpr.keyword("generated-files")
  generatedFiles: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object NobleIdlGenerationResult {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.NobleIdlGenerationResult, _root_.dev.argon.nobleidl.compiler.api.NobleIdlGenerationResult] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.NobleIdlGenerationResult, _root_.dev.argon.nobleidl.compiler.api.NobleIdlGenerationResult] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.NobleIdlGenerationResult): _root_.dev.argon.nobleidl.compiler.api.NobleIdlGenerationResult = {
        new _root_.dev.argon.nobleidl.compiler.api.NobleIdlGenerationResult(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).toJava(s_value.generatedFiles),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.NobleIdlGenerationResult): _root_.nobleidl.compiler.api.NobleIdlGenerationResult = {
        _root_.nobleidl.compiler.api.NobleIdlGenerationResult(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).fromJava(j_value.generatedFiles().nn),
        )
      }
    }
}
