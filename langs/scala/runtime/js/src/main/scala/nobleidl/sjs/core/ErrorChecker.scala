package nobleidl.sjs.core

import scala.scalajs.js

trait ErrorChecker[E] extends js.Object {
  def isInstance(x: js.Any): Boolean
}
