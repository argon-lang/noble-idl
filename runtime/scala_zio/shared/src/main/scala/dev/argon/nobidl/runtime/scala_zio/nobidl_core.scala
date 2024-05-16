
package dev.argon.nobidl.scala_zio.core

type String = java.lang.String
type Binary = zio.Chunk[Byte]

type Int = scala.math.BigInt
type Nat = scala.math.BigInt

type I8 = Byte
type U8 = Byte
type I16 = Short
type U16 = Short
type I32 = scala.Int
type U32 = scala.Int
type I64 = Long
type U64 = Long

type F32 = Float
type F64 = Double

type Unit = scala.Unit

type List[A] = scala.Seq[A]
type Option[A] = scala.Option[A]

