package dev.argon.nobidl.runtime.scala_zio

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

trait JavaTypeAdapter[A, B] {
  def toJava(a: A): B
  def fromJava(b: B): A
}
