package nobleidl.core

import scala.reflect.TypeTest

trait ErrorType[E] {
  def checkObject(o: Any): Option[E]
  def checkThrowable(ex: Throwable): Option[E]
  
  def throwableIsError(ex: Throwable): Boolean = checkThrowable(ex).isDefined
  def objectIsError(o: Any): Boolean = checkObject(o).isDefined
}

object ErrorType extends ErrorTypePlatformSpecific {  
  given [E <: Throwable](using tt: TypeTest[Any, E]): ErrorType[E] with
    override def checkObject(o: Any): Option[E] =
      o.asInstanceOf[Matchable] match {
        case o: E => Some(o)
        case _ => None
      }

    override def checkThrowable(ex: Throwable): Option[E] =
      ex match {
        case ex: E => Some(ex)
        case _ => None
      }
  end given
}

