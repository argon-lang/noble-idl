package nobleidl.compiler.api
@_root_.esexpr.constructor("qualified-name")
final case class QualifiedName(
  `package`: _root_.nobleidl.compiler.api.PackageName,
  name: _root_.nobleidl.core.String,
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
