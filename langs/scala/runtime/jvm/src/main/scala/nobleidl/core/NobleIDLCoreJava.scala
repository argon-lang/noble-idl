package nobleidl.core

import dev.argon.esexpr.KeywordMapping
import esexpr.{Dictionary, ESExpr}
import dev.argon.nobleidl.runtime as jcore
import esexpr.unsigned.*

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import java.util.{List as JList, Optional as JOptional}

trait EsexprObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[Esexpr, dev.argon.esexpr.ESExpr] =
    new JavaAdapter[Esexpr, dev.argon.esexpr.ESExpr] {
      override def toJava(s: Esexpr): dev.argon.esexpr.ESExpr =
        ESExpr.toJava(s)

      override def fromJava(j: dev.argon.esexpr.ESExpr): Esexpr =
        ESExpr.fromJava(j)
    }
}

trait StringObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[String, String] =
    JavaAdapter.identity
}

trait BinaryObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[Binary, Array[Byte]] =
    new JavaAdapter[Binary, Array[Byte]] {
      override def toJava(s: Binary): Array[Byte] = IArray.genericWrapArray(s.array).toArray
      override def fromJava(j: Array[Byte]): Binary = Binary(IArray(j*))
    }
}

trait IntObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[scala.math.BigInt, java.math.BigInteger] =
    new JavaAdapter[scala.math.BigInt, java.math.BigInteger] {
      override def toJava(s: scala.math.BigInt): java.math.BigInteger = s.bigInteger
      override def fromJava(j: java.math.BigInteger): scala.math.BigInt = j
    }
}

trait NatObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[scala.math.BigInt, java.math.BigInteger] =
    Int.javaAdapter()
}

trait BoolObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[Bool, Boolean] =
    JavaAdapter.identity

  def javaAdapterBoxed(): JavaAdapter[Bool, java.lang.Boolean] =
    new JavaAdapter[Bool, java.lang.Boolean] {
      override def toJava(s: Bool): java.lang.Boolean = s
      override def fromJava(j: java.lang.Boolean): Bool = j
    }
}

trait I8ObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[I8, Byte] =
    JavaAdapter.identity

  def javaAdapterBoxed(): JavaAdapter[I8, java.lang.Byte] =
    new JavaAdapter[I8, java.lang.Byte] {
      override def toJava(s: I8): java.lang.Byte = s
      override def fromJava(j: java.lang.Byte): I8 = j
    }
}

trait U8ObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[U8, Byte] =
    new JavaAdapter[U8, Byte] {
      override def toJava(s: U8): Byte = s.toByte
      override def fromJava(j: Byte): U8 = j.toUByte
    }

  def javaAdapterBoxed(): JavaAdapter[U8, java.lang.Byte] =
    new JavaAdapter[U8, java.lang.Byte] {
      override def toJava(s: U8): java.lang.Byte = s.toByte
      override def fromJava(j: java.lang.Byte): U8 = j.toUByte
    }
}

trait I16ObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[I16, Short] =
    JavaAdapter.identity

  def javaAdapterBoxed(): JavaAdapter[I16, java.lang.Short] =
    new JavaAdapter[I16, java.lang.Short] {
      override def toJava(s: I16): java.lang.Short = s
      override def fromJava(j: java.lang.Short): I16 = j
    }
}

trait U16ObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[U16, Short] =
    new JavaAdapter[U16, Short] {
      override def toJava(s: U16): Short = s.toShort
      override def fromJava(j: Short): U16 = j.toUShort
    }

  def javaAdapterBoxed(): JavaAdapter[U16, java.lang.Short] =
    new JavaAdapter[U16, java.lang.Short] {
      override def toJava(s: U16): java.lang.Short = s.toShort
      override def fromJava(j: java.lang.Short): U16 = j.toUShort
    }
}

trait I32ObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[I32, scala.Int] =
    JavaAdapter.identity

  def javaAdapterBoxed(): JavaAdapter[I32, java.lang.Integer] =
    new JavaAdapter[I32, java.lang.Integer] {
      override def toJava(s: I32): java.lang.Integer = s
      override def fromJava(j: java.lang.Integer): I32 = j
    }
}

trait U32ObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[U32, scala.Int] =
    new JavaAdapter[U32, scala.Int] {
      override def toJava(s: U32): scala.Int = s.toInt
      override def fromJava(j: scala.Int): U32 = j.toUInt
    }

  def javaAdapterBoxed(): JavaAdapter[U32, java.lang.Integer] =
    new JavaAdapter[U32, java.lang.Integer] {
      override def toJava(s: U32): java.lang.Integer = s.toInt
      override def fromJava(j: java.lang.Integer): U32 = j.toUInt
    }
}

trait I64ObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[I64, Long] =
    JavaAdapter.identity

  def javaAdapterBoxed(): JavaAdapter[I64, java.lang.Long] =
    new JavaAdapter[I64, java.lang.Long] {
      override def toJava(s: I64): java.lang.Long = s
      override def fromJava(j: java.lang.Long): I64 = j
    }
}

trait U64ObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[U64, Long] =
    new JavaAdapter[U64, Long] {
      override def toJava(s: U64): Long = s.toLong
      override def fromJava(j: Long): U64 = j.toULong
    }

  def javaAdapterBoxed(): JavaAdapter[U64, java.lang.Long] =
    new JavaAdapter[U64, java.lang.Long] {
      override def toJava(s: U64): java.lang.Long = s.toLong
      override def fromJava(j: java.lang.Long): U64 = j.toULong
    }
}

trait F32ObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[F32, Float] =
    JavaAdapter.identity

  def javaAdapterBoxed(): JavaAdapter[F32, java.lang.Float] =
    new JavaAdapter[F32, java.lang.Float] {
      override def toJava(s: F32): java.lang.Float = s
      override def fromJava(j: java.lang.Float): F32 = j
    }
}

trait F64ObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[F64, Double] =
    JavaAdapter.identity

  def javaAdapterBoxed(): JavaAdapter[F64, java.lang.Double] =
    new JavaAdapter[F64, java.lang.Double] {
      override def toJava(s: F64): java.lang.Double = s
      override def fromJava(j: java.lang.Double): F64 = j
    }
}

trait UnitObjectPlatformSpecific {
  def javaAdapter(): JavaAdapter[Unit, Unit] =
    JavaAdapter.identity

  def javaAdapterBoxed(): JavaAdapter[Unit, AnyRef] =
    new JavaAdapter[Unit, AnyRef] {
      override def toJava(s: Unit): AnyRef = new AnyRef()
      override def fromJava(j: AnyRef): Unit = ()
    }
}

trait ListObjectPlatformSpecific {
  def javaAdapter[SA, JA](aAdapter: JavaAdapter[SA, JA]): JavaAdapter[Seq[SA], JList[JA]] =
    new JavaAdapter[Seq[SA], JList[JA]] {
      override def toJava(s: Seq[SA]): JList[JA] =
        s.map(aAdapter.toJava).asJava

      override def fromJava(j: JList[JA]): Seq[SA] =
        j.asScala.view.map(aAdapter.fromJava).toSeq
    }
}

trait OptionObjectPlatformSpecific {
  def javaAdapter[SA, JA](aAdapter: JavaAdapter[SA, JA]): JavaAdapter[Option[SA], JOptional[JA]] =
    new JavaAdapter[Option[SA], JOptional[JA]] {
      override def toJava(s: Option[SA]): JOptional[JA] =
        s.map(aAdapter.toJava).toJava

      override def fromJava(j: JOptional[JA]): Option[SA] =
        j.toScala.map(aAdapter.fromJava)
    }
}

trait OptionalFieldObjectPlatformSpecific {
  def javaAdapter[SA, JA](aAdapter: JavaAdapter[SA, JA]): JavaAdapter[Option[SA], JOptional[JA]] =
    Option.javaAdapter(aAdapter)
}

trait DictObjectPlatformSpecific {
  def javaAdapter[SA, JA](aAdapter: JavaAdapter[SA, JA]): JavaAdapter[Dict[SA], KeywordMapping[JA]] =
    new JavaAdapter[Dict[SA], KeywordMapping[JA]] {
      override def toJava(s: Dict[SA]): KeywordMapping[JA] =
        KeywordMapping[JA](
          s.dict
            .view
            .mapValues(aAdapter.toJava)
            .toMap
            .asJava
        )

      override def fromJava(j: KeywordMapping[JA]): Dict[SA] =
        Dictionary(
          j.map.asScala
            .view
            .mapValues(aAdapter.fromJava)
            .toMap
        )
    }
}

