package nobleidl.core

import dev.argon.util.async.JSPromiseUtil
import zio.*

import scala.scalajs.js
import scala.scalajs.js.JavaScriptException

object JSIOInterop {

  def runJS[SE, JE, SA, JA](
    errorAdapter: JSAdapter[SE, JE],
    resultAdapter: JSAdapter[SA, JA],
    errorType: nobleidl.core.ErrorType[JE],
  )(f: => js.Promise[JA]): IO[SE, SA] =
    ZIO.fromPromiseJS(f)
      .catchAll { ex =>
        errorType.checkThrowable(ex).fold(ZIO.die(ex))(e => ZIO.fail(errorAdapter.fromJS(e)))
      }
      .map(resultAdapter.fromJS)

  def runJS[SA, JA](
    resultAdapter: JSAdapter[SA, JA],
  )(f: => js.Promise[JA]): UIO[SA] =
    ZIO.fromPromiseJS(f)
      .orDie
      .map(resultAdapter.fromJS)

  def runScala[SE, JE, SA, JA](
    errorAdapter: JSAdapter[SE, JE],
    resultAdapter: JSAdapter[SA, JA],
  )(f: IO[SE, SA])(using rt: Runtime[Any]): js.Promise[JA] =
    JSPromiseUtil.runEffectToPromiseRaw(
      f
        .map(resultAdapter.toJS)
        .mapError { ex =>
          errorAdapter.toJS(ex).asInstanceOf[Matchable] match {
            case jsEx: Throwable => jsEx
            case jsEx => JavaScriptException(jsEx)
          }
        }
    )

  def runScala[SA, JA](
    resultAdapter: JSAdapter[SA, JA],
  )(f: UIO[SA])(using rt: Runtime[Any]): js.Promise[JA] =
    JSPromiseUtil.runEffectToPromiseRaw(f.map(resultAdapter.toJS))

}
