package nobleidl.compiler.api
@_root_.esexpr.constructor("package-name")
final case class PackageName(
  @_root_.esexpr.vararg
  parts: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
