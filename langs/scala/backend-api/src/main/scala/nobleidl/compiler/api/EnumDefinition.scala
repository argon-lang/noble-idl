package nobleidl.compiler.api
@_root_.esexpr.constructor("enum-definition")
final case class EnumDefinition(
  @_root_.esexpr.vararg
  cases: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.EnumCase],
  @_root_.esexpr.keyword("esexpr-options")
  @_root_.esexpr.optional
  esexprOptions: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.EsexprEnumOptions],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object EnumDefinition {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EnumDefinition, _root_.dev.argon.nobleidl.compiler.api.EnumDefinition] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.EnumDefinition, _root_.dev.argon.nobleidl.compiler.api.EnumDefinition] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.EnumDefinition): _root_.dev.argon.nobleidl.compiler.api.EnumDefinition = {
        new _root_.dev.argon.nobleidl.compiler.api.EnumDefinition(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.EnumCase, _root_.dev.argon.nobleidl.compiler.api.EnumCase](_root_.nobleidl.compiler.api.EnumCase.javaAdapter()).toJava(s_value.cases),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprEnumOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprEnumOptions](_root_.nobleidl.compiler.api.EsexprEnumOptions.javaAdapter()).toJava(s_value.esexprOptions),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.EnumDefinition): _root_.nobleidl.compiler.api.EnumDefinition = {
        _root_.nobleidl.compiler.api.EnumDefinition(
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.EnumCase, _root_.dev.argon.nobleidl.compiler.api.EnumCase](_root_.nobleidl.compiler.api.EnumCase.javaAdapter()).fromJava(j_value.cases().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprEnumOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprEnumOptions](_root_.nobleidl.compiler.api.EsexprEnumOptions.javaAdapter()).fromJava(j_value.esexprOptions().nn),
        )
      }
    }
}
