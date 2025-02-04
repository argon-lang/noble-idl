package nobleidl.core

import zio.{Cause, FiberId}

import scala.reflect.TypeTest
import java.util.WeakHashMap

trait ErrorType[E <: Throwable] {
  given errorTypeTest: TypeTest[Throwable, E]
}

object ErrorType extends ErrorTypePlatformSpecific {
  given [E <: Throwable](using tt: TypeTest[Throwable, E]): ErrorType[E] with
    override def errorTypeTest: TypeTest[Throwable, E] = tt
  end given
}

