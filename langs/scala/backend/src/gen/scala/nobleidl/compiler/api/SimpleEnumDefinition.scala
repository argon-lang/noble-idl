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
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.SimpleEnumDefinition, _root_.dev.argon.nobleidl.compiler.api.SimpleEnumDefinition] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.SimpleEnumDefinition, _root_.dev.argon.nobleidl.compiler.api.SimpleEnumDefinition] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.SimpleEnumDefinition): _root_.dev.argon.nobleidl.compiler.api.SimpleEnumDefinition = {
        new _root_.dev.argon.nobleidl.compiler.api.SimpleEnumDefinition(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.SimpleEnumCase, _root_.dev.argon.nobleidl.compiler.api.SimpleEnumCase](_root_.nobleidl.compiler.api.SimpleEnumCase.javaAdapter()).toJava(s_value.cases),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprSimpleEnumOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumOptions](_root_.nobleidl.compiler.api.EsexprSimpleEnumOptions.javaAdapter()).toJava(s_value.esexprOptions),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.SimpleEnumDefinition): _root_.nobleidl.compiler.api.SimpleEnumDefinition = {
        _root_.nobleidl.compiler.api.SimpleEnumDefinition(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.SimpleEnumCase, _root_.dev.argon.nobleidl.compiler.api.SimpleEnumCase](_root_.nobleidl.compiler.api.SimpleEnumCase.javaAdapter()).fromJava(j_value.cases().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprSimpleEnumOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumOptions](_root_.nobleidl.compiler.api.EsexprSimpleEnumOptions.javaAdapter()).fromJava(j_value.esexprOptions().nn),
        )
      }
    }
}
