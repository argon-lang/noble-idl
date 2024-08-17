package nobleidl.compiler.api
enum NobleIdlCompileModelResult derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual {
  @_root_.esexpr.constructor("success")
  case Success(
    model: _root_.nobleidl.compiler.api.NobleIdlModel,
  )
  @_root_.esexpr.constructor("failure")
  case Failure(
    @_root_.esexpr.vararg
    errors: _root_.nobleidl.core.List[_root_.nobleidl.core.String],
  )
}
object NobleIdlCompileModelResult {
}
