package nobleidl.compiler.api
@_root_.esexpr.constructor("simple-enum-case")
final case class SimpleEnumCase(
  name: _root_.nobleidl.core.String,
  @_root_.esexpr.keyword("esexpr-options")
  @_root_.esexpr.optional
  esexprOptions: _root_.nobleidl.core.OptionalField[_root_.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions],
  @_root_.esexpr.keyword("annotations")
  annotations: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.Annotation],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object SimpleEnumCase {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.SimpleEnumCase, _root_.dev.argon.nobleidl.compiler.api.SimpleEnumCase] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.SimpleEnumCase, _root_.dev.argon.nobleidl.compiler.api.SimpleEnumCase] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.SimpleEnumCase): _root_.dev.argon.nobleidl.compiler.api.SimpleEnumCase = {
        new _root_.dev.argon.nobleidl.compiler.api.SimpleEnumCase(
          _root_.nobleidl.core.String.javaAdapter().toJava(s_value.name),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions](_root_.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions.javaAdapter()).toJava(s_value.esexprOptions),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).toJava(s_value.annotations),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.SimpleEnumCase): _root_.nobleidl.compiler.api.SimpleEnumCase = {
        _root_.nobleidl.compiler.api.SimpleEnumCase(
          _root_.nobleidl.core.String.javaAdapter().fromJava(j_value.name().nn),
          _root_.nobleidl.core.OptionalField.javaAdapter[_root_.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions, _root_.dev.argon.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions](_root_.nobleidl.compiler.api.EsexprSimpleEnumCaseOptions.javaAdapter()).fromJava(j_value.esexprOptions().nn),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).fromJava(j_value.annotations().nn),
        )
      }
    }
}
