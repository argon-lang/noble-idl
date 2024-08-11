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
}
