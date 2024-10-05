package nobleidl.core
@_root_.esexpr.constructor("dict")
final case class DictRepr[A](
  @_root_.esexpr.dict
  values: _root_.nobleidl.core.Dict[A],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object DictRepr {
  def javaAdapter[SA, JA](aAdapter: _root_.nobleidl.core.JavaAdapter[SA, JA]): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.core.DictRepr[SA], _root_.dev.argon.nobleidl.runtime.DictRepr[JA]] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.core.DictRepr[SA], _root_.dev.argon.nobleidl.runtime.DictRepr[JA]] {
      override def toJava(s_value: _root_.nobleidl.core.DictRepr[SA]): _root_.dev.argon.nobleidl.runtime.DictRepr[JA] = {
        new _root_.dev.argon.nobleidl.runtime.DictRepr[JA](
          _root_.nobleidl.core.Dict.javaAdapter[SA, JA](aAdapter).toJava(s_value.values),
        )
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.runtime.DictRepr[JA]): _root_.nobleidl.core.DictRepr[SA] = {
        _root_.nobleidl.core.DictRepr[SA](
          _root_.nobleidl.core.Dict.javaAdapter[SA, JA](aAdapter).fromJava(j_value.values().nn),
        )
      }
    }
}
