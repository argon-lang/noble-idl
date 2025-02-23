package nobleidl.compiler.api.java
enum JavaAnnExternType derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("mapped-to")
  case MappedTo(
    javaType: _root_.nobleidl.compiler.api.java.JavaMappedType,
  )
}
object JavaAnnExternType {
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.java.JavaAnnExternType, _root_.dev.argon.nobleidl.compiler.api.java.JavaAnnExternType] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.java.JavaAnnExternType, _root_.dev.argon.nobleidl.compiler.api.java.JavaAnnExternType] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.java.JavaAnnExternType): _root_.dev.argon.nobleidl.compiler.api.java.JavaAnnExternType = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.java.JavaAnnExternType.MappedTo =>
            new _root_.dev.argon.nobleidl.compiler.api.java.JavaAnnExternType.MappedTo(
              _root_.nobleidl.compiler.api.java.JavaMappedType.javaAdapter().toJava(s_value.javaType),
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.java.JavaAnnExternType): _root_.nobleidl.compiler.api.java.JavaAnnExternType = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.java.JavaAnnExternType.MappedTo =>
            new _root_.nobleidl.compiler.api.java.JavaAnnExternType.MappedTo(
              _root_.nobleidl.compiler.api.java.JavaMappedType.javaAdapter().fromJava(j_value.javaType().nn),
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
