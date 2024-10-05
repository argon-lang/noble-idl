package nobleidl.core
@_root_.esexpr.constructor("dict")
final case class DictRepr[A](
  @_root_.esexpr.dict
  values: _root_.nobleidl.core.Dict[A],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object DictRepr {
  def jsAdapter[SA, JA](aAdapter: _root_.nobleidl.core.JSAdapter[SA, JA]): _root_.nobleidl.core.JSAdapter[_root_.nobleidl.core.DictRepr[SA], _root_.nobleidl.sjs.core.DictRepr[JA]] =
    new _root_.nobleidl.core.JSAdapter[_root_.nobleidl.core.DictRepr[SA], _root_.nobleidl.sjs.core.DictRepr[JA]] {
      override def toJS(s_value: _root_.nobleidl.core.DictRepr[SA]): _root_.nobleidl.sjs.core.DictRepr[JA] = {
        new _root_.nobleidl.sjs.core.DictRepr[JA] {
          override val values: _root_.nobleidl.sjs.core.Dict[JA] = _root_.nobleidl.core.Dict.jsAdapter[SA, JA](aAdapter).toJS(s_value.values)
        }
      }
      override def fromJS(j_value: _root_.nobleidl.sjs.core.DictRepr[JA]): _root_.nobleidl.core.DictRepr[SA] = {
        _root_.nobleidl.core.DictRepr[SA](
          _root_.nobleidl.core.Dict.jsAdapter[SA, JA](aAdapter).fromJS(j_value.values),
        )
      }
    }
}
