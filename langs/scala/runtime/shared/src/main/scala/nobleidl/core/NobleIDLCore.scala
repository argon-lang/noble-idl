package nobleidl.core

type Esexpr = esexpr.ESExpr
object Esexpr extends EsexprObjectPlatformSpecific

type String = java.lang.String
object String extends StringObjectPlatformSpecific

type Binary = IArray[Byte]
object Binary extends BinaryObjectPlatformSpecific

type Int = scala.math.BigInt
object Int extends IntObjectPlatformSpecific

type Nat = scala.math.BigInt
object Nat extends NatObjectPlatformSpecific

type Bool = scala.Boolean
object Bool extends BoolObjectPlatformSpecific {
  def fromBool(b: scala.Boolean): Bool = b
}

type I8 = scala.Byte
object I8 extends I8ObjectPlatformSpecific
type U8 = esexpr.unsigned.UByte
object U8 extends U8ObjectPlatformSpecific
type I16 = scala.Short
object I16 extends I16ObjectPlatformSpecific
type U16 = esexpr.unsigned.UShort
object U16 extends U16ObjectPlatformSpecific
type I32 = scala.Int
object I32 extends I32ObjectPlatformSpecific
type U32 = esexpr.unsigned.UInt
object U32 extends U32ObjectPlatformSpecific
type I64 = scala.Long
object I64 extends I64ObjectPlatformSpecific
type U64 = esexpr.unsigned.ULong
object U64 extends U64ObjectPlatformSpecific

type F32 = scala.Float
object F32 extends F32ObjectPlatformSpecific
type F64 = scala.Double
object F64 extends F64ObjectPlatformSpecific


type Unit = scala.Unit
object Unit extends UnitObjectPlatformSpecific

type List[+A] = scala.Seq[A]
object List extends ListObjectPlatformSpecific

type Option[+A] = scala.Option[A]
object Option extends OptionObjectPlatformSpecific

type OptionalField[+A] = scala.Option[A]
object OptionalField extends OptionalFieldObjectPlatformSpecific

type Dict[+A] = esexpr.Dictionary[A]
object Dict extends DictObjectPlatformSpecific
