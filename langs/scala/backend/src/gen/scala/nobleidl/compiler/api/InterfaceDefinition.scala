package nobleidl.compiler.api
@_root_.esexpr.constructor("interface-definition")
final case class InterfaceDefinition(
  @_root_.esexpr.vararg
  methods: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.InterfaceMethod],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
