package nobleidl.core

import scala.reflect.TypeTest

trait ErrorTypePlatformSpecific {

  private final case class JavaErrorTypeWrapper[E <: Throwable](errorType: ErrorType[E]) extends dev.argon.nobleidl.runtime.ErrorType[E] {
    import errorType.errorTypeTest

    override def tryFromThrowable(x: Throwable): E | Null =
      x match {
        case x: E => x
        case _ => null
      }
  }

  private final case class WrappedJavaErrorType[E <: Throwable](errorType: dev.argon.nobleidl.runtime.ErrorType[E]) extends ErrorType[E] {
    override def errorTypeTest: TypeTest[Throwable, E] =
        new TypeTest[Throwable, E] {
          override def unapply(x: Throwable): _root_.scala.Option[x.type & E] =
            val e = errorType.tryFromThrowable(x)
            if java.util.Objects.isNull(e) then
              None
            else
              Some(e.asInstanceOf[x.type & E])
          end unapply
        }
  }


  def fromJavaErrorType[E <: Throwable](errorType: dev.argon.nobleidl.runtime.ErrorType[E]): ErrorType[E] =
    errorType match {
      case JavaErrorTypeWrapper(errorType) => errorType
      case _ => WrappedJavaErrorType(errorType)
    }

  def toJavaErrorType[E <: Throwable](errorType: ErrorType[E]): dev.argon.nobleidl.runtime.ErrorType[E] =
    errorType match {
      case WrappedJavaErrorType(errorType) => errorType
      case _ => JavaErrorTypeWrapper(errorType)
    }
}

