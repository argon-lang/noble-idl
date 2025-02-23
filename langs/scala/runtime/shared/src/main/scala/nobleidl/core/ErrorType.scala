package nobleidl.core

import scala.reflect.TypeTest
import scala.util.NotGiven

trait ErrorType[E] {
  def checkObject(o: Any): Option[E]
  def checkThrowable(ex: Throwable): Option[E]
  
  def throwableIsError(ex: Throwable): Boolean = checkThrowable(ex).isDefined
  def objectIsError(o: Any): Boolean = checkObject(o).isDefined
}

object ErrorType extends ErrorTypePlatformSpecific {  
  given fromTypeTest[E <: Throwable](using tt: TypeTest[Any, E]): ErrorType[E] with
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
  end fromTypeTest

  given fromTypeTestThrowable[E <: Throwable](using tt: TypeTest[Throwable, E]): ErrorType[E] =
    fromTypeTest(using new TypeTest[Any, E] {
      override def unapply(x: Any): Option[x.type & E] =
        summon[TypeTest[Any, Throwable]].unapply(x) match {
          case Some(y) => tt.unapply(y)
          case _: None.type => None
        }
    })
}

