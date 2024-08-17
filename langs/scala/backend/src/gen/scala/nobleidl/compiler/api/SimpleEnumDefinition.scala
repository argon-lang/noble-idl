package nobleidl.compiler.api
@_root_.esexpr.constructor("simple-enum-definition")
final case class SimpleEnumDefinition(
  @_root_.esexpr.vararg
  cases: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.SimpleEnumCase],
  @_root_.esexpr.keyword("esexpr-options")
  @_root_.esexpr.optional
  esexprOptions: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.EsexprSimpleEnumOptions],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object SimpleEnumDefinition {
}
