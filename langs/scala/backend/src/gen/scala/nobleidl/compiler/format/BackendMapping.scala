package nobleidl.compiler.format
@_root_.esexpr.constructor("backends")
final case class BackendMapping(
  @_root_.esexpr.dict
  mapping: _root_.nobleidl.core.Dict[_root_.nobleidl.compiler.format.BackendOptions],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
