package nobleidl.compiler.api.java
enum JavaAnnExternType derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("mapped-to")
  case MappedTo(
    javaType: _root_.nobleidl.compiler.api.java.JavaMappedType,
  )
}
object JavaAnnExternType {
}
