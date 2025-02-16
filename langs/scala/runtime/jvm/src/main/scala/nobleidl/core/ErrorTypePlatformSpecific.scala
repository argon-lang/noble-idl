package nobleidl.core

import scala.reflect.TypeTest

trait ErrorTypePlatformSpecific {

  private final case class JavaErrorTypeWrapper[E <: Throwable](errorType: ErrorType[E]) extends dev.argon.nobleidl.runtime.ErrorType[E] {
    override def tryFromThrowable(x: Throwable): E | Null =
      errorType.checkError(x).orNull
  }

  private final case class WrappedJavaErrorType[+E <: Throwable](errorType: dev.argon.nobleidl.runtime.ErrorType[? <: E]) extends ErrorType[E] {
    override def checkError(ex: Throwable): Option[E] =
      val e = errorType.tryFromThrowable(ex)
      if e eq null then
        None
      else
        Some(e)
    end checkError
  }


  def fromJavaErrorType[E <: Throwable](errorType: dev.argon.nobleidl.runtime.ErrorType[? <: E]): ErrorType[E] =
    errorType match {
      case JavaErrorTypeWrapper(errorType) => errorType
      case _ => WrappedJavaErrorType(errorType)
    }

  def toJavaErrorType[E <: Throwable](errorType: ErrorType[E]): dev.argon.nobleidl.runtime.ErrorType[? <: E] =
    errorType match {
      case WrappedJavaErrorType(errorType) => errorType
      case _ => JavaErrorTypeWrapper(errorType)
    }
}

