package nobleidl.compiler.api
@_root_.esexpr.simple
enum TypeParameterOwner derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("by-type")
  case ByType
  @_root_.esexpr.constructor("by-method")
  case ByMethod
}
object TypeParameterOwner {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.TypeParameterOwner, _root_.dev.argon.nobleidl.compiler.api.TypeParameterOwner] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.TypeParameterOwner, _root_.dev.argon.nobleidl.compiler.api.TypeParameterOwner] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.TypeParameterOwner): _root_.dev.argon.nobleidl.compiler.api.TypeParameterOwner = {
        s_value match {
          case _root_.nobleidl.compiler.api.TypeParameterOwner.ByType =>
            _root_.dev.argon.nobleidl.compiler.api.TypeParameterOwner.BY_TYPE
          case _root_.nobleidl.compiler.api.TypeParameterOwner.ByMethod =>
            _root_.dev.argon.nobleidl.compiler.api.TypeParameterOwner.BY_METHOD
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.TypeParameterOwner): _root_.nobleidl.compiler.api.TypeParameterOwner = {
        j_value match {case _: _root_.dev.argon.nobleidl.compiler.api.TypeParameterOwner.BY_TYPE
          .type =>
            _root_.nobleidl.compiler.api.TypeParameterOwner.ByType
          case _: _root_.dev.argon.nobleidl.compiler.api.TypeParameterOwner.BY_METHOD
          .type =>
            _root_.nobleidl.compiler.api.TypeParameterOwner.ByMethod
        }
      }
    }
}
