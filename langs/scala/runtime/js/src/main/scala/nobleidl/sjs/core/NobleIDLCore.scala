package nobleidl.sjs.core

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

type Esexpr = esexpr.sjs.ESExpr
type String = java.lang.String
type Binary = Uint8Array
type Int = js.BigInt
type Nat = js.BigInt
type Bool = scala.Boolean
type I8 = scala.Byte
type U8 = scala.Short
type I16 = scala.Short
type U16 = scala.Int
type I32 = scala.Int
type U32 = scala.Double
type I64 = js.BigInt
type U64 = js.BigInt
type F32 = scala.Float
type F64 = scala.Double

type Unit = scala.Unit
type List[A] = js.Array[A]
type Option[A] = OptionSome[A] | Null
trait OptionSome[A] extends js.Object {
  val value: A
}
type OptionalField[A] = js.UndefOr[A]
type Dict[A] = js.Map[String, A]

