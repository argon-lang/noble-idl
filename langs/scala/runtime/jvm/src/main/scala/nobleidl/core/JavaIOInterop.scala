package nobleidl.core

import zio.*
import dev.argon.util.async.JavaExecuteIO

object JavaIOInterop {
  def runJava[SE <: Throwable, JE <: Throwable, SA, JA](
    errorAdapter: JavaAdapter[SE, JE],
    resultAdapter: JavaAdapter[SA, JA],
    errorType: nobleidl.core.ErrorType[JE],
  )(f: => JA): IO[SE, SA] =
    import errorType.errorTypeTest
    JavaExecuteIO.runJavaRaw(f)
      .catchAll {
        case ex: JE => ZIO.fail(errorAdapter.fromJava(ex))
        case ex => ZIO.die(ex)
      }
      .map(resultAdapter.fromJava)
  end runJava

  def runJava[SA, JA](
    resultAdapter: JavaAdapter[SA, JA],
  )(f: => JA): UIO[SA] =
    JavaExecuteIO.runJavaRaw(f)
      .orDie
      .map(resultAdapter.fromJava)

  def runScala[SE <: Throwable, JE <: Throwable, SA, JA](
    errorAdapter: JavaAdapter[SE, JE],
    resultAdapter: JavaAdapter[SA, JA],
  )(f: IO[SE, SA])(using rt: Runtime[Any]): JA =
    JavaExecuteIO.runInterruptableRaw(
      f
        .map(resultAdapter.toJava)
        .mapError(errorAdapter.toJava)
    )

  def runScala[SA, JA](
    resultAdapter: JavaAdapter[SA, JA],
  )(f: UIO[SA])(using rt: Runtime[Any]): JA =
    JavaExecuteIO.runInterruptableRaw(
      f.map(resultAdapter.toJava)
    )

}
