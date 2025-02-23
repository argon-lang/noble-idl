package nobleidl.compiler.api
@_root_.esexpr.constructor("options")
final case class NobleIdlCompileModelOptions(
  @_root_.esexpr.keyword("library-files")
  libraryFiles: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
  @_root_.esexpr.keyword("files")
  files: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object NobleIdlCompileModelOptions {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.NobleIdlCompileModelOptions, _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelOptions] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.NobleIdlCompileModelOptions, _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelOptions] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.NobleIdlCompileModelOptions): _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelOptions = {
        new _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelOptions(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).toJava(s_value.libraryFiles),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).toJava(s_value.files),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelOptions): _root_.nobleidl.compiler.api.NobleIdlCompileModelOptions = {
        _root_.nobleidl.compiler.api.NobleIdlCompileModelOptions(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).fromJava(j_value.libraryFiles().nn),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).fromJava(j_value.files().nn),
        )
      }
    }
}
