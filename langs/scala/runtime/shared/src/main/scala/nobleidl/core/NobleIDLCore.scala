package nobleidl.core

import esexpr.{ESExprCodec, ESExprTag}

import scala.compiletime.asMatchable

type Esexpr = esexpr.ESExpr
object Esexpr extends EsexprObjectPlatformSpecific

type String = java.lang.String
object String extends StringObjectPlatformSpecific {
  def fromString(s: java.lang.String): String = s
}

final case class Binary(val array: IArray[Byte]) extends AnyVal derives CanEqual {
  override def hashCode(): scala.Int =
    array.toSeq.hashCode()

  override def equals(obj: scala.Any): scala.Boolean =
    obj.asMatchable match {
      case other: Binary => array.toSeq.equals(other.array.toSeq)
      case _ => false
    }
}
object Binary extends BinaryObjectPlatformSpecific {
  def fromBinary(b: IArray[Byte]): Binary = Binary(b)

  given ESExprCodec[Binary]:
    private val innerCodec = summon[ESExprCodec[IArray[Byte]]]
    
    override lazy val tags: Set[ESExprTag] =
      innerCodec.tags
    
    override def encode(value: Binary): Esexpr =
      innerCodec.encode(value.array)

    override def decode(expr: Esexpr): Either[ESExprCodec.DecodeError, Binary] =
      innerCodec.decode(expr).map(Binary.apply)
  end given
}

type Int = scala.math.BigInt
object Int extends IntObjectPlatformSpecific {
  def fromBigInt(i: scala.math.BigInt): Int = i
}

type Nat = scala.math.BigInt
object Nat extends NatObjectPlatformSpecific {
  def fromBigInt(i: scala.math.BigInt): Int = i
}

type Bool = scala.Boolean
object Bool extends BoolObjectPlatformSpecific {
  def fromBoolean(b: scala.Boolean): Bool = b
}

type I8 = scala.Byte
object I8 extends I8ObjectPlatformSpecific {
  def fromByte(i: scala.Byte): I8 = i
}
type U8 = esexpr.unsigned.UByte
object U8 extends U8ObjectPlatformSpecific {
  def fromUByte(i: esexpr.unsigned.UByte): U8 = i
}
type I16 = scala.Short
object I16 extends I16ObjectPlatformSpecific {
  def fromShort(i: scala.Short): I16 = i
}
type U16 = esexpr.unsigned.UShort
object U16 extends U16ObjectPlatformSpecific {
  def fromUShort(i: esexpr.unsigned.UShort): U16 = i
}
type I32 = scala.Int
object I32 extends I32ObjectPlatformSpecific {
  def fromInt(i: scala.Int): I32 = i
}
type U32 = esexpr.unsigned.UInt
object U32 extends U32ObjectPlatformSpecific {
  def fromUInt(i: esexpr.unsigned.UInt): U32 = i
}
type I64 = scala.Long
object I64 extends I64ObjectPlatformSpecific {
  def fromLong(i: scala.Long): I64 = i
}
type U64 = esexpr.unsigned.ULong
object U64 extends U64ObjectPlatformSpecific {
  def fromULong(i: esexpr.unsigned.ULong): U64 = i
}

type F32 = scala.Float
object F32 extends F32ObjectPlatformSpecific {
  def fromFloat(f: scala.Float): F32 = f
}
type F64 = scala.Double
object F64 extends F64ObjectPlatformSpecific {
  def fromDouble(f: scala.Double): F64 = f
}


type Unit = scala.Unit
object Unit extends UnitObjectPlatformSpecific

type List[+A] = scala.Seq[A]
object List extends ListObjectPlatformSpecific {
  def fromSeq[A](values: Seq[A]): List[A] = values
  
  def buildFrom[A](repr: ListRepr[A]): List[A] =
    repr.values
}

type Option[+A] = scala.Option[A]
object Option extends OptionObjectPlatformSpecific {
  def buildFrom[A](value: A): Option[A] = Some(value)
  def fromNull[A]: Option[A] = None 
}

type OptionalField[+A] = scala.Option[A]
object OptionalField extends OptionalFieldObjectPlatformSpecific {
  def fromOptional[A](o: scala.Option[A]): OptionalField[A] = o
}

type Dict[+A] = esexpr.Dictionary[A]
object Dict extends DictObjectPlatformSpecific {
  def buildFrom[A](repr: DictRepr[A]): Dict[A] = repr.values
  def fromMap[A](map: scala.collection.immutable.Map[String, A]): Dict[A] = esexpr.Dictionary(map)
}
