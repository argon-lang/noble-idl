package nobleidl.core

import nobleidl.sjs.core.ErrorChecker

import scala.scalajs.js
import scala.scalajs.js.JavaScriptException

trait ErrorTypePlatformSpecific {
  private final class JSErrorTypeWrapper[E](val errorType: ErrorType[E]) extends ErrorChecker[E] {
    override def isInstance(x: Any): Bool =
      errorType.objectIsError(x.asInstanceOf[Matchable])
  }

  private final class WrappedJSErrorChecker[E](val errorChecker: ErrorChecker[E]) extends ErrorType[E] {
    override def checkObject(o: Any): Option[E] =
      if errorChecker.isInstance(o) then
        Some(o.asInstanceOf[E])
      else
        None

    override def checkThrowable(ex: Throwable): Option[E] =
      ex match {
        case JavaScriptException(e) =>
          if errorChecker.isInstance(e) then
            Some(e.asInstanceOf[E])
          else
            None

        case _ =>
          if errorChecker.isInstance(ex) then
            Some(ex.asInstanceOf[E])
          else
            None
      }
  }
  
  def fromJSErrorChecker[E](errorChecker: ErrorChecker[E]): ErrorType[E] =
    errorChecker match {
      case errorChecker: JSErrorTypeWrapper[E] => errorChecker.errorType
      case _ => WrappedJSErrorChecker(errorChecker)
    }
    
  def toJSErrorChecker[E](errorType: ErrorType[E]): ErrorChecker[E] =
    errorType match {
      case errorType: WrappedJSErrorChecker[E] => errorType.errorChecker
      case _ => JSErrorTypeWrapper(errorType)
    }
}

