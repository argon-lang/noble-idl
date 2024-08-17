package nobleidl.core
@_root_.esexpr.constructor("list")
final case class ListRepr[A](
  @_root_.esexpr.vararg
  values: _root_.nobleidl.core.List[A],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object ListRepr {
  def javaAdapter[SA, JA](aAdapter: _root_.nobleidl.core.JavaAdapter[SA, JA]): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.core.ListRepr[SA], _root_.dev.argon.nobleidl.runtime.ListRepr[JA]] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.core.ListRepr[SA], _root_.dev.argon.nobleidl.runtime.ListRepr[JA]] {
      override def toJava(s_value: _root_.nobleidl.core.ListRepr[SA]): _root_.dev.argon.nobleidl.runtime.ListRepr[JA] = {
        new _root_.dev.argon.nobleidl.runtime.ListRepr[JA](
          _root_.nobleidl.core.List.javaAdapter[SA, JA](aAdapter).toJava(s_value.values),
        )
      }
      override def fromJava(j: _root_.dev.argon.nobleidl.runtime.ListRepr[JA]): _root_.nobleidl.core.ListRepr[SA] = {
        _root_.nobleidl.core.ListRepr[SA](
          _root_.nobleidl.core.List.javaAdapter[SA, JA](aAdapter).fromJava(j.values().nn),
        )
      }
    }
}
