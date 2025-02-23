package nobleidl.compiler.api
@_root_.esexpr.constructor("extern-type-definition")
final case class ExternTypeDefinition(
  @_root_.esexpr.keyword("esexpr-options")
  @_root_.esexpr.optional
  esexprOptions: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.EsexprExternTypeOptions],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object ExternTypeDefinition {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.ExternTypeDefinition, _root_.dev.argon.nobleidl.compiler.api.ExternTypeDefinition] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.ExternTypeDefinition, _root_.dev.argon.nobleidl.compiler.api.ExternTypeDefinition] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.ExternTypeDefinition): _root_.dev.argon.nobleidl.compiler.api.ExternTypeDefinition = {
        new _root_.dev.argon.nobleidl.compiler.api.ExternTypeDefinition(
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprExternTypeOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeOptions](_root_.nobleidl.compiler.api.EsexprExternTypeOptions.javaAdapter()).toJava(s_value.esexprOptions),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.ExternTypeDefinition): _root_.nobleidl.compiler.api.ExternTypeDefinition = {
        _root_.nobleidl.compiler.api.ExternTypeDefinition(
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprExternTypeOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprExternTypeOptions](_root_.nobleidl.compiler.api.EsexprExternTypeOptions.javaAdapter()).fromJava(j_value.esexprOptions().nn),
        )
      }
    }
}
