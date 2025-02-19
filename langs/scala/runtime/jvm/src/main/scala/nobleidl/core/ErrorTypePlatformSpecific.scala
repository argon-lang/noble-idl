package nobleidl.core

import java.io.IOException
import scala.reflect.TypeTest

trait ErrorTypePlatformSpecific {

  private type ErrorToException[E] = (E & Throwable) | WrappedValueException[E]
  private type ErrorToIOException[E] = (E & IOException) | WrappedValueIOException[E]

  private final case class WrappedValueException[E](value: E) extends Exception
  private final case class WrappedValueIOException[E](value: E) extends IOException

  private final case class JavaErrorTypeWrapper[E](errorType: ErrorType[E]) extends dev.argon.nobleidl.runtime.ErrorType[E, ErrorToException[E]] {
    override def tryFromObject(o: Any): E | Null =
      errorType.checkObject(o.asInstanceOf[Matchable]).orNull

    override def tryFromThrowable(x: Throwable): E | Null =
      x match {
        case WrappedValueException(wrappedValue) => tryFromObject(wrappedValue)
        case _ => errorType.checkThrowable(x).orNull
      }

    override def toThrowable(t: E): ErrorToException[E] =
      t.asInstanceOf[E & Matchable] match {
        case t: Throwable => t
        case _ => WrappedValueException(t)
      }
  }

  private final case class JavaErrorTypeWrapperIO[E](errorType: ErrorType[E]) extends dev.argon.nobleidl.runtime.util.IOErrorType[E, ErrorToIOException[E]] {
    override def tryFromObject(o: Any): E | Null =
      errorType.checkObject(o.asInstanceOf[Matchable]).orNull

    override def tryFromThrowable(x: Throwable): E | Null =
      x match {
        case WrappedValueException(wrappedValue) => tryFromObject(wrappedValue)
        case _ => errorType.checkThrowable(x).orNull
      }

    override def toThrowable(t: E): ErrorToIOException[E] =
      t.asInstanceOf[E & Matchable] match {
        case t: IOException => t
        case _ => WrappedValueIOException(t)
      }
  }

  private final case class WrappedJavaErrorType[E](errorType: dev.argon.nobleidl.runtime.ErrorType[E, ?]) extends ErrorType[E] {
    override def checkObject(o: Any): Option[E] =
      val e = errorType.tryFromObject(o)
      if java.util.Objects.isNull(e) then
        None
      else
        Some(e)
    end checkObject

    override def checkThrowable(ex: Throwable): Option[E] =
      val e = errorType.tryFromThrowable(ex)
      if java.util.Objects.isNull(e) then
        None
      else
        Some(e)
    end checkThrowable
  }


  def fromJavaErrorType[E](errorType: dev.argon.nobleidl.runtime.ErrorType[E, ?]): ErrorType[E] =
    errorType match {
      case JavaErrorTypeWrapper(errorType) => errorType
      case JavaErrorTypeWrapperIO(errorType) => errorType
      case _ => WrappedJavaErrorType(errorType)
    }

  def toJavaErrorType[E](errorType: ErrorType[E]): dev.argon.nobleidl.runtime.ErrorType[E, ?] =
    errorType match {
      case WrappedJavaErrorType(errorType) => errorType
      case _ => JavaErrorTypeWrapper(errorType)
    }

  def toJavaIOErrorType[E](errorType: ErrorType[E]): dev.argon.nobleidl.runtime.util.IOErrorType[E, ?] =
    errorType match {
      case WrappedJavaErrorType(errorType: dev.argon.nobleidl.runtime.util.IOErrorType[E, ?]) => errorType
      case _ => JavaErrorTypeWrapperIO(errorType)
    }
}

