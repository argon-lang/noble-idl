package nobleidl.compiler.api
@_root_.esexpr.constructor("options")
final case class NobleIdlCompileModelOptions(
  @_root_.esexpr.keyword("library-files")
  libraryFiles: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
  @_root_.esexpr.keyword("files")
  files: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
