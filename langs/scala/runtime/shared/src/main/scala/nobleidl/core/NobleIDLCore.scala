package nobleidl.core

type Esexpr = esexpr.ESExpr
type String = java.lang.String
type Binary = IArray[Byte]
type Int = scala.math.BigInt
type Nat = scala.math.BigInt

type Bool = scala.Boolean
object Bool {
  def fromBool(b: scala.Boolean): Bool = b
}

type I8 = scala.Byte
type U8 = esexpr.unsigned.UByte
type I16 = scala.Short
type U16 = esexpr.unsigned.UShort
type I32 = scala.Int
type U32 = esexpr.unsigned.UInt
type I64 = scala.Long
type U64 = esexpr.unsigned.ULong
type F32 = scala.Float
type F64 = scala.Double
type Unit = scala.Unit
type List[+A] = scala.Seq[A]
type Option[+A] = scala.Option[A]
type OptionalField[+A] = scala.Option[A]
type Dict[+A] = esexpr.Dictionary[A]
