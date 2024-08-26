package nobleidl.core

import zio.{Cause, FiberId}

import scala.reflect.TypeTest

trait ErrorWrapper[E] extends dev.argon.util.async.ErrorWrapper[E] {
  type JavaClass <: Throwable
  override type EX >: JavaClass <: Throwable
  val javaClass: Class[JavaClass]
}

object ErrorWrapper {
  def fromJavaClass[E <: Throwable](jClass: Class[E]): ErrorWrapper[E] =
    new ErrorWrapper[E] {
      override type JavaClass = E
      override type EX = E
      override val javaClass: Class[E] = jClass

      override def exceptionTypeTest: TypeTest[Throwable, EX] =
        new TypeTest[Throwable, EX] {
          override def unapply(x: Throwable): _root_.scala.Option[x.type & EX] =
            x match {
              case _ if javaClass.isInstance(x) => Some(x.asInstanceOf[x.type & EX])
              case _ => None
            }
        }

      override def wrap(error: Cause[E]): EX =
        error.failureOption.get


      override def unwrap(ex: EX): Cause[E] =
        Cause.fail(ex)
    }
}

