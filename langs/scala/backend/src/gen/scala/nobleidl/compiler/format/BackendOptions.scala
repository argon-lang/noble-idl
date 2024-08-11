package nobleidl.compiler.format
@_root_.esexpr.constructor("backend-options")
final case class BackendOptions(
  @_root_.esexpr.keyword("package-mapping")
  packageMapping: _root_.nobleidl.compiler.format.PackageMapping,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
