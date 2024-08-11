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
