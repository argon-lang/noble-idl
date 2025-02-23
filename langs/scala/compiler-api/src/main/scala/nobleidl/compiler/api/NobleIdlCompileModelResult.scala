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
  def javaAdapter(): _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.NobleIdlCompileModelResult, _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult] =
    new _root_.nobleidl.core.JavaAdapter[_root_.nobleidl.compiler.api.NobleIdlCompileModelResult, _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult] {
      override def toJava(s_value: _root_.nobleidl.compiler.api.NobleIdlCompileModelResult): _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult = {
        s_value match {
          case s_value: _root_.nobleidl.compiler.api.NobleIdlCompileModelResult.Success =>
            new _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult.Success(
              _root_.nobleidl.compiler.api.NobleIdlModel.javaAdapter().toJava(s_value.model),
            )
          case s_value: _root_.nobleidl.compiler.api.NobleIdlCompileModelResult.Failure =>
            new _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult.Failure(
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).toJava(s_value.errors),
            )
        }
      }
      override def fromJava(j_value: _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult): _root_.nobleidl.compiler.api.NobleIdlCompileModelResult = {
        j_value match {
          case j_value: _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult.Success =>
            new _root_.nobleidl.compiler.api.NobleIdlCompileModelResult.Success(
              _root_.nobleidl.compiler.api.NobleIdlModel.javaAdapter().fromJava(j_value.model().nn),
            )
          case j_value: _root_.dev.argon.nobleidl.compiler.api.NobleIdlCompileModelResult.Failure =>
            new _root_.nobleidl.compiler.api.NobleIdlCompileModelResult.Failure(
              _root_.nobleidl.core.List.javaAdapter[_root_.nobleidl.core.String, _root_.java.lang.String](_root_.nobleidl.core.String.javaAdapter()).fromJava(j_value.errors().nn),
            )
          case _ => throw new _root_.scala.MatchError(j_value)
        }
      }
    }
}
