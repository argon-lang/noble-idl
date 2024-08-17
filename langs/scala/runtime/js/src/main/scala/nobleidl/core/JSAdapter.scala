package nobleidl.core

trait JSAdapter[S, J] {
  def toJS(s: S): J
  def fromJS(j: J): S
}

object JSAdapter {
  def identity[A]: JSAdapter[A, A] =
    new JSAdapter[A, A] {
      override def toJS(s: A): A = s
      override def fromJS(j: A): A = j
    }
}

