package nobleidl.compiler.api
enum Definition derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.inlineValue
  case Record(
    r: _root_.nobleidl.compiler.api.RecordDefinition,
  )
  @_root_.esexpr.inlineValue
  case Enum(
    e: _root_.nobleidl.compiler.api.EnumDefinition,
  )
  @_root_.esexpr.inlineValue
  case SimpleEnum(
    e: _root_.nobleidl.compiler.api.SimpleEnumDefinition,
  )
  @_root_.esexpr.inlineValue
  case ExternType(
    et: _root_.nobleidl.compiler.api.ExternTypeDefinition,
  )
  @_root_.esexpr.inlineValue
  case Interface(
    iface: _root_.nobleidl.compiler.api.InterfaceDefinition,
  )
  @_root_.esexpr.inlineValue
  case ExceptionType(
    ex: _root_.nobleidl.compiler.api.ExceptionTypeDefinition,
  )
}
object Definition {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.Definition, _root_.dev.argon.nobleidl.compiler.api.Definition] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.Definition, _root_.dev.argon.nobleidl.compiler.api.Definition] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.Definition): _root_.dev.argon.nobleidl.compiler.api.Definition = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.Definition.Record =>
            new _root_.dev.argon.nobleidl.compiler.api.Definition.Record(
              _root_.nobleidl.compiler.api.RecordDefinition.javaAdapter().toJava(s_value.r),
            )
          case s_value: _root_.nobleidl.compiler.api.Definition.Enum =>
            new _root_.dev.argon.nobleidl.compiler.api.Definition.Enum(
              _root_.nobleidl.compiler.api.EnumDefinition.javaAdapter().toJava(s_value.e),
            )
          case s_value: _root_.nobleidl.compiler.api.Definition.SimpleEnum =>
            new _root_.dev.argon.nobleidl.compiler.api.Definition.SimpleEnum(
              _root_.nobleidl.compiler.api.SimpleEnumDefinition.javaAdapter().toJava(s_value.e),
            )
          case s_value: _root_.nobleidl.compiler.api.Definition.ExternType =>
            new _root_.dev.argon.nobleidl.compiler.api.Definition.ExternType(
              _root_.nobleidl.compiler.api.ExternTypeDefinition.javaAdapter().toJava(s_value.et),
            )
          case s_value: _root_.nobleidl.compiler.api.Definition.Interface =>
            new _root_.dev.argon.nobleidl.compiler.api.Definition.Interface(
              _root_.nobleidl.compiler.api.InterfaceDefinition.javaAdapter().toJava(s_value.iface),
            )
          case s_value: _root_.nobleidl.compiler.api.Definition.ExceptionType =>
            new _root_.dev.argon.nobleidl.compiler.api.Definition.ExceptionType(
              _root_.nobleidl.compiler.api.ExceptionTypeDefinition.javaAdapter().toJava(s_value.ex),
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.Definition): _root_.nobleidl.compiler.api.Definition = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.Definition.Record =>
            new _root_.nobleidl.compiler.api.Definition.Record(
              _root_.nobleidl.compiler.api.RecordDefinition.javaAdapter().fromJava(j_value.r().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.Definition.Enum =>
            new _root_.nobleidl.compiler.api.Definition.Enum(
              _root_.nobleidl.compiler.api.EnumDefinition.javaAdapter().fromJava(j_value.e().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.Definition.SimpleEnum =>
            new _root_.nobleidl.compiler.api.Definition.SimpleEnum(
              _root_.nobleidl.compiler.api.SimpleEnumDefinition.javaAdapter().fromJava(j_value.e().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.Definition.ExternType =>
            new _root_.nobleidl.compiler.api.Definition.ExternType(
              _root_.nobleidl.compiler.api.ExternTypeDefinition.javaAdapter().fromJava(j_value.et().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.Definition.Interface =>
            new _root_.nobleidl.compiler.api.Definition.Interface(
              _root_.nobleidl.compiler.api.InterfaceDefinition.javaAdapter().fromJava(j_value.iface().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.Definition.ExceptionType =>
            new _root_.nobleidl.compiler.api.Definition.ExceptionType(
              _root_.nobleidl.compiler.api.ExceptionTypeDefinition.javaAdapter().fromJava(j_value.ex().nn),
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
