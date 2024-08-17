package nobleidl.core
@_root_.esexpr.constructor("list")
final case class ListRepr[A](
  @_root_.esexpr.vararg
  values: _root_.nobleidl.core.List[A],
) derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual
object ListRepr {
}
