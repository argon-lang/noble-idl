package dev.argon.nobidl.sjs.core

import scala.scalajs.js
import scala.scalajs.js.typedarray.{*, given}
import scala.scalajs.js.JSConverters.*
import scala.collection.mutable

trait JSTypeAdapter[A, B] {
  def toJS(a: A): B
  def fromJS(b: B): A
}

object JSTypeAdapter {
  private[core] def identity[A]: JSTypeAdapter[A, A] = new JSTypeAdapter[A, A] {
    override def toJS(a: A): A = a
    override def fromJS(b: A): A = b
  }
}

type String = java.lang.String
object String {
  val jsTypeAdapter: JSTypeAdapter[String, String] = JSTypeAdapter.identity
}

type Binary = Uint8Array
object Binary {
  val jsTypeAdapter: JSTypeAdapter[zio.Chunk[Byte], js.typedarray.Uint8Array] =
    new JSTypeAdapter[zio.Chunk[Byte], js.typedarray.Uint8Array] {
      override def toJS(a: zio.Chunk[Byte]): Uint8Array =
        val ta = a.toArray.toTypedArray
        new Uint8Array(ta.buffer, ta.byteOffset, ta.length)
      end toJS

      override def fromJS(b: Uint8Array): zio.Chunk[Byte] =
        zio.Chunk.fromArray(new Int8Array(b.buffer, b.byteOffset, b.length).toArray)
    }
}

type Int = js.BigInt
object Int {
  val jsTypeAdapter: JSTypeAdapter[scala.math.BigInt, js.BigInt] =
    new JSTypeAdapter[scala.math.BigInt, js.BigInt] {
    override def toJS(a: scala.math.BigInt): js.BigInt = js.BigInt(a.toString)
    override def fromJS(b: js.BigInt): scala.math.BigInt = scala.math.BigInt(b.toString)
    }
}

type Nat = js.BigInt
object Nat {
  export Int.jsTypeAdapter
}

type I8 = Byte
object I8 {
  val jsTypeAdapter: JSTypeAdapter[Byte, Byte] = JSTypeAdapter.identity
}

type U8 = scala.Int
object U8 {
  val jsTypeAdapter: JSTypeAdapter[Byte, scala.Int] =
    new JSTypeAdapter[Byte, scala.Int] {
      override def toJS(a: Byte): scala.Int = java.lang.Byte.toUnsignedInt(a)
      override def fromJS(b: scala.Int): Byte = b.toByte
    }
}

type I16 = Short
object I16 {
  val jsTypeAdapter: JSTypeAdapter[Short, Short] = JSTypeAdapter.identity
}

type U16 = scala.Int
object U16 {
  val jsTypeAdapter: JSTypeAdapter[Short, scala.Int] =
    new JSTypeAdapter[Short, scala.Int] {
      override def toJS(a: Short): scala.Int = java.lang.Short.toUnsignedInt(a)
      override def fromJS(b: scala.Int): Short = b.toByte
    }
}


type I32 = scala.Int
object I32 {
  val jsTypeAdapter: JSTypeAdapter[scala.Int, scala.Int] = JSTypeAdapter.identity
}

type U32 = Double
object U32 {
  val jsTypeAdapter: JSTypeAdapter[scala.Int, Double] =
    new JSTypeAdapter[scala.Int, Double] {
      override def toJS(a: scala.Int): Double = java.lang.Integer.toUnsignedLong(a).toDouble
      override def fromJS(b: Double): scala.Int = b.toLong.toInt
    }
}

type I64 = js.BigInt
object I64 {
  val jsTypeAdapter: JSTypeAdapter[Long, js.BigInt] =
    new JSTypeAdapter[Long, js.BigInt] {
      override def toJS(a: Long): js.BigInt = js.BigInt(a.toString)
      override def fromJS(b: js.BigInt): Long = b.toString.toLong
    }
}

type U64 = js.BigInt
object U64 {
  val jsTypeAdapter: JSTypeAdapter[Long, js.BigInt] =
    new JSTypeAdapter[Long, js.BigInt] {
      override def toJS(a: Long): js.BigInt = js.BigInt(java.lang.Long.toUnsignedString(a).nn)
      override def fromJS(b: js.BigInt): Long = java.lang.Long.parseUnsignedLong(b.toString)
    }
}



type F32 = Float
object F32 {
  val jsTypeAdapter: JSTypeAdapter[Float, Float] = JSTypeAdapter.identity
}

type F64 = Double
object F64 {
  val jsTypeAdapter: JSTypeAdapter[Double, Double] = JSTypeAdapter.identity
}

type Unit = scala.Unit
object Unit {
  val jsTypeAdapter: JSTypeAdapter[Unit, Unit] = JSTypeAdapter.identity
}

type List[A] = js.Array[A]
object List {
  def jsTypeAdapter[A1, A2](elementAdapter: JSTypeAdapter[A1, A2]): JSTypeAdapter[Seq[A1], js.Array[A2]] =
    new JSTypeAdapter[Seq[A1], js.Array[A2]] {
      override def toJS(a: Seq[A1]): js.Array[A2] = a.view.map(elementAdapter.toJS).toJSArray
      override def fromJS(b: js.Array[A2]): Seq[A1] = (b : mutable.Seq[A2]).view.map(elementAdapter.fromJS).toSeq
    }
}

trait JSSome[A] extends js.Object {
    val value: A
}

type Option[A] = JSSome[A] | Null
object Option {
  def jsTypeAdapter[A1, A2](elementAdapter: JSTypeAdapter[A1, A2]): JSTypeAdapter[scala.Option[A1], Option[A2]] =
    new JSTypeAdapter[scala.Option[A1], Option[A2]] {
      override def toJS(a: scala.Option[A1]): Option[A2] = a.map(a1 => new JSSome[A2] { val value: A2 = elementAdapter.toJS(a1) }).orNull
      override def fromJS(b: Option[A2]): scala.Option[A1] =
        if b == null then
          None
        else
          Some(elementAdapter.fromJS(b.value))
    }
}

