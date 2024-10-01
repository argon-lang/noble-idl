package nobleidl.compiler

import esexpr.*
import scala.CanEqual
import nobleidl.core.Dict

@constructor("package-mapping")
final case class PackageMapping(
  @dict
  mapping: Dict[String],
) derives ESExprCodec, CanEqual

