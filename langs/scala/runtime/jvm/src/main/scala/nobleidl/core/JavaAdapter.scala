package nobleidl.core

trait JavaAdapter[S, J] {
  def toJava(s: S): J
  def fromJava(j: J): S
}

object JavaAdapter {
  def identity[A]: JavaAdapter[A, A] =
    new JavaAdapter[A, A] {
      override def toJava(s: A): A = s
      override def fromJava(j: A): A = j
    }
}
