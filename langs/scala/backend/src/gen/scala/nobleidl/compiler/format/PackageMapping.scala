package nobleidl.compiler.format
@_root_.esexpr.constructor("package-mapping")
final case class PackageMapping(
  @_root_.esexpr.dict
  mapping: _root_.nobleidl.core.Dict[_root_.nobleidl.core.String],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
