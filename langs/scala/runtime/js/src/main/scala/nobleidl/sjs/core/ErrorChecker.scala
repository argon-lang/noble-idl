package nobleidl.sjs.core

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, JSName}

trait NobleIdlError[Name <: String] extends js.Error {
  @JSName(ErrorChecker.nobleidlErrorTypeSymbol)
  val errorType: Name
  val information: Any
}

trait ErrorChecker[E] extends js.Object {
  def isInstance(x: Any): Boolean
}

@JSImport("@argon-lang/noble-idl-core/util")
@js.native
object ErrorChecker extends js.Object {
  val nobleidlErrorTypeSymbol: js.Symbol = js.native
  
  def fromTypeName[Name <: String, E <: NobleIdlError[Name]](name: Name): ErrorChecker[E] = js.native
}
