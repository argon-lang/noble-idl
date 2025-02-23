package nobleidl.compiler.api
@_root_.esexpr.constructor("definition-info")
final case class DefinitionInfo(
  @_root_.esexpr.keyword("name")
  name: _root_.nobleidl.compiler.api.QualifiedName,
  @_root_.esexpr.keyword("type-parameters")
  typeParameters: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.TypeParameter],
  @_root_.esexpr.keyword("definition")
  definition: _root_.nobleidl.compiler.api.Definition,
  @_root_.esexpr.keyword("annotations")
  annotations: _root_.nobleidl.core.List[_root_.nobleidl.compiler.api.Annotation],
  @_root_.esexpr.keyword("is-library")
  isLibrary: _root_.nobleidl.core.Bool,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object DefinitionInfo {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.DefinitionInfo, _root_.dev.argon.nobleidl.compiler.api.DefinitionInfo] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.DefinitionInfo, _root_.dev.argon.nobleidl.compiler.api.DefinitionInfo] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.DefinitionInfo): _root_.dev.argon.nobleidl.compiler.api.DefinitionInfo = {
        new _root_.dev.argon.nobleidl.compiler.api.DefinitionInfo(
          _root_.nobleidl.compiler.api.QualifiedName.javaAdapter().toJava(s_value.name),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.TypeParameter, _root_.dev.argon.nobleidl.compiler.api.TypeParameter](_root_.nobleidl.compiler.api.TypeParameter.javaAdapter()).toJava(s_value.typeParameters),
          _root_.nobleidl.compiler.api.Definition.javaAdapter().toJava(s_value.definition),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).toJava(s_value.annotations),
          _root_.nobleidl.core.Bool.javaAdapter().toJava(s_value.isLibrary),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.DefinitionInfo): _root_.nobleidl.compiler.api.DefinitionInfo = {
        _root_.nobleidl.compiler.api.DefinitionInfo(
          _root_.nobleidl.compiler.api.QualifiedName.javaAdapter().fromJava(j_value.name().nn),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.TypeParameter, _root_.dev.argon.nobleidl.compiler.api.TypeParameter](_root_.nobleidl.compiler.api.TypeParameter.javaAdapter()).fromJava(j_value.typeParameters().nn),
          _root_.nobleidl.compiler.api.Definition.javaAdapter().fromJava(j_value.definition().nn),
          _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.compiler.api.Annotation, _root_.dev.argon.nobleidl.compiler.api.Annotation](_root_.nobleidl.compiler.api.Annotation.javaAdapter()).fromJava(j_value.annotations().nn),
          _root_.nobleidl.core.Bool.javaAdapter().fromJava(j_value.isLibrary().nn),
        )
      }
    }
}
