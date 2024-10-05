package nobleidl.core

import dev.argon.util.async.TypedArrayUtil
import esexpr.{Dictionary, ESExpr}
import esexpr.unsigned.*

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.UndefOr

trait EsexprObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[Esexpr, esexpr.sjs.ESExpr] =
    new JSAdapter[Esexpr, esexpr.sjs.ESExpr] {
      override def toJS(s: Esexpr): esexpr.sjs.ESExpr =
        ESExpr.toJS(s)

      override def fromJS(j: esexpr.sjs.ESExpr): Esexpr =
        ESExpr.fromJS(j)
    }
}

trait StringObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[String, String] =
    JSAdapter.identity
}

trait BinaryObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[Binary, Uint8Array] =
    new JSAdapter[Binary, Uint8Array] {
      override def toJS(s: Binary): Uint8Array = TypedArrayUtil.fromByteArray(IArray.genericWrapArray(s.array).toArray)
      override def fromJS(j: Uint8Array): Binary = Binary(IArray(TypedArrayUtil.toByteArray(j)*))
    }
}

trait IntObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[scala.math.BigInt, js.BigInt] =
    new JSAdapter[scala.math.BigInt, js.BigInt] {
      override def toJS(s: scala.math.BigInt): js.BigInt = js.BigInt(s.toString)
      override def fromJS(j: js.BigInt): scala.math.BigInt = BigInt(j.toString)
    }
}

trait NatObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[scala.math.BigInt, js.BigInt] =
    Int.jsAdapter()
}

trait BoolObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[Bool, Boolean] =
    JSAdapter.identity
}

trait I8ObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[I8, Byte] =
    JSAdapter.identity
}

trait U8ObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[U8, Short] =
    new JSAdapter[U8, Short] {
      override def toJS(s: U8): Short = s.toShort
      override def fromJS(j: Short): U8 = j.toUByte
    }
}

trait I16ObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[I16, Short] =
    JSAdapter.identity
}

trait U16ObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[U16, scala.Int] =
    new JSAdapter[U16, scala.Int] {
      override def toJS(s: U16): scala.Int = s.toInt
      override def fromJS(j: scala.Int): U16 = j.toUShort
    }
}

trait I32ObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[I32, scala.Int] =
    JSAdapter.identity
}

trait U32ObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[U32, Double] =
    new JSAdapter[U32, Double] {
      override def toJS(s: U32): Double = s.toLong.toDouble
      override def fromJS(j: Double): U32 = j.toLong.toUInt
    }
}

trait I64ObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[I64, js.BigInt] =
    new JSAdapter[I64, js.BigInt] {
      override def toJS(s: I64): js.BigInt = js.BigInt(s.toString)
      override def fromJS(j: js.BigInt): I64 = j.toString.toLong
    }
}

trait U64ObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[U64, js.BigInt] =
    new JSAdapter[U64, js.BigInt] {
      override def toJS(s: U64): js.BigInt = js.BigInt(s.toLong.toString)
      override def fromJS(j: js.BigInt): U64 = BigInt(j.toString).toULong
    }
}

trait F32ObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[F32, Float] =
    JSAdapter.identity
}

trait F64ObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[F64, Double] =
    JSAdapter.identity
}

trait UnitObjectPlatformSpecific {
  def jsAdapter(): JSAdapter[Unit, Unit] =
    JSAdapter.identity
}

trait ListObjectPlatformSpecific {
  def jsAdapter[SA, JA](aAdapter: JSAdapter[SA, JA]): JSAdapter[Seq[SA], js.Array[JA]] =
    new JSAdapter[Seq[SA], js.Array[JA]] {
      override def toJS(s: Seq[SA]): js.Array[JA] =
        s.map(aAdapter.toJS).toJSArray

      override def fromJS(j: js.Array[JA]): Seq[SA] =
        j.view.map(aAdapter.fromJS).toSeq
    }
}

trait OptionObjectPlatformSpecific {
  def jsAdapter[SA, JA](aAdapter: JSAdapter[SA, JA]): JSAdapter[Option[SA], nobleidl.sjs.core.Option[JA]] =
    new JSAdapter[Option[SA], nobleidl.sjs.core.Option[JA]] {
      override def toJS(s: Option[SA]): nobleidl.sjs.core.Option[JA] =
        s match {
          case Some(v) =>
            val ja = aAdapter.toJS(v)
            new nobleidl.sjs.core.OptionSome[JA] {
              override val value: JA = ja
            }

          case _: None.type =>
            null
        }

      override def fromJS(j: nobleidl.sjs.core.Option[JA]): Option[SA] =
        if j eq null then
          None
        else
          Some(aAdapter.fromJS(j.value))
    }
}

trait OptionalFieldObjectPlatformSpecific {
  def jsAdapter[SA, JA](aAdapter: JSAdapter[SA, JA]): JSAdapter[Option[SA], js.UndefOr[JA]] =
    new JSAdapter[Option[SA], js.UndefOr[JA]] {
      override def toJS(s: Option[SA]): UndefOr[JA] =
        s.map(aAdapter.toJS).orUndefined

      override def fromJS(j: UndefOr[JA]): Option[SA] =
        j.toOption.map(aAdapter.fromJS)
    }
}

trait DictObjectPlatformSpecific {
  def jsAdapter[SA, JA](aAdapter: JSAdapter[SA, JA]): JSAdapter[Dict[SA], js.Map[String, JA]] =
    new JSAdapter[Dict[SA], js.Map[String, JA]] {
      override def toJS(s: Dict[SA]): js.Map[String, JA] =
        s.dict
          .view
          .mapValues(aAdapter.toJS)
          .toMap
          .toJSMap

      override def fromJS(j: js.Map[String, JA]): Dict[SA] =
        Dictionary(
          j
            .view
            .mapValues(aAdapter.fromJS)
            .toMap
        )
    }
}

