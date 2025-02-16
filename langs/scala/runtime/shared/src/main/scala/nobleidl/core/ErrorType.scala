package nobleidl.core

import zio.{Cause, FiberId}

import scala.reflect.TypeTest
import java.util.WeakHashMap

trait ErrorType[+E <: Throwable] {
  def checkError(ex: Throwable): Option[E]
}

object ErrorType extends ErrorTypePlatformSpecific {
  given [E <: Throwable](using tt: TypeTest[Throwable, E]): ErrorType[E] with
    override def checkError(ex: Throwable): Option[E] =
      ex match {
        case ex: E => Some(ex)
        case _ => None
      }
  end given
}

