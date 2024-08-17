package nobleidl.core
@_root_.esexpr.constructor("list")
final case class ListRepr[A](
  @_root_.esexpr.vararg
  values: _root_.nobleidl.core.List[A],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object ListRepr {
  def jsAdapter[SA, JA](aAdapter: _root_.nobleidl.core.JSAdapter[SA, JA]): _root_.nobleidl.core.JSAdapter[_root_.nobleidl.core.ListRepr[SA], _root_.nobleidl.sjs.core.ListRepr[JA]] =
    new _root_.nobleidl.core.JSAdapter[_root_.nobleidl.core.ListRepr[SA], _root_.nobleidl.sjs.core.ListRepr[JA]] {
      override def toJS(s_value: _root_.nobleidl.core.ListRepr[SA]): _root_.nobleidl.sjs.core.ListRepr[JA] = {
        new _root_.nobleidl.sjs.core.ListRepr[JA] {
          override val values: _root_.nobleidl.sjs.core.List[JA] = _root_.nobleidl.core.List.jsAdapter[SA, JA](aAdapter).toJS(s_value.values)
        }
      }
      override def fromJS(j: _root_.nobleidl.sjs.core.ListRepr[JA]): _root_.nobleidl.core.ListRepr[SA] = {
        _root_.nobleidl.core.ListRepr[SA](
          _root_.nobleidl.core.List.jsAdapter[SA, JA](aAdapter).fromJS(j.values),
        )
      }
    }
}
