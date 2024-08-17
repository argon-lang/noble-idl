package nobleidl.compiler.api
@_root_.esexpr.simple
enum TypeParameterOwner derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("by-type")case ByType
  @_root_.esexpr.constructor("by-method")case ByMethod
}
object TypeParameterOwner {
}
