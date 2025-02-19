package nobleidl.core

import zio.*
import dev.argon.util.async.JavaExecuteIO

object JavaIOInterop {
  def runJava[SE, JE, SA, JA](
    errorAdapter: JavaAdapter[SE, JE],
    resultAdapter: JavaAdapter[SA, JA],
    errorType: nobleidl.core.ErrorType[JE],
  )(f: => JA): IO[SE, SA] =
    JavaExecuteIO.runJavaRaw(f)
      .catchAll { ex =>
          errorType.checkThrowable(ex).fold(ZIO.die(ex))(e => ZIO.fail(errorAdapter.fromJava(e)))
      }
      .map(resultAdapter.fromJava)

  def runJava[SA, JA](
    resultAdapter: JavaAdapter[SA, JA],
  )(f: => JA): UIO[SA] =
    JavaExecuteIO.runJavaRaw(f)
      .orDie
      .map(resultAdapter.fromJava)

  def runScala[SE, JE, SA, JA](
    errorAdapter: JavaAdapter[SE, JE],
    resultAdapter: JavaAdapter[SA, JA],
    errorType: dev.argon.nobleidl.runtime.ErrorType[JE, ? <: Throwable],
  )(f: IO[SE, SA])(using rt: Runtime[Any]): JA =
    JavaExecuteIO.runInterruptableRaw(
      f
        .map(resultAdapter.toJava)
        .mapError(errorAdapter.toJava)
        .mapError(errorType.toThrowable)
    )

  def runScala[SA, JA](
    resultAdapter: JavaAdapter[SA, JA],
  )(f: UIO[SA])(using rt: Runtime[Any]): JA =
    JavaExecuteIO.runInterruptableRaw(
      f.map(resultAdapter.toJava)
    )

}
