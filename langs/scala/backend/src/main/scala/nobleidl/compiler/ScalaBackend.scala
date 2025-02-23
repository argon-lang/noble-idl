package nobleidl.compiler

import esexpr.ESExprCodec
import esexpr.unsigned.{UByte, UInt, ULong, UShort}
import nobleidl.compiler
import nobleidl.compiler.CodeWriter.Operations.*
import nobleidl.compiler.api.{TypeExpr, java as _, *}
import nobleidl.compiler.PackageMapping
import org.apache.commons.text.StringEscapeUtils
import zio.*
import zio.stream.*
import scala.reflect.TypeTest

import _root_.java.nio.file.Path
import _root_.java.util.Locale
import nobleidl.compiler.api.java.{JavaAnnExternType, JavaMappedType}
import nobleidl.compiler.api.scalaAnns.*

import javax.lang.model.SourceVersion
import scala.jdk.CollectionConverters.*
import dev.argon.nobleidl.compiler.NobleIDLCompileErrorException

final class ScalaBackend(genRequest: NobleIdlGenerationRequest[ScalaLanguageOptions]) extends ScalaBackendBase {
  import ScalaBackendBase.*

  private val options: ScalaLanguageOptions = genRequest.languageOptions

  protected override def model: NobleIdlModel = genRequest.model
  protected override def packageMappingRaw: PackageMapping = options.packageMapping

  protected override def emitRecord(dfn: DefinitionInfo, r: RecordDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- ZIO.foreachDiscard(r.esexprOptions) { esexprOptions =>
        for
          _ <- write("@_root_.esexpr.constructor(\"")
          _ <- write(StringEscapeUtils.escapeJava(esexprOptions.constructor).nn)
          _ <- writeln("\")")
        yield ()
      }
      _ <- write("final case class ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeTypeParameters(dfn.typeParameters)
      _ <- writeCaseClassParameters(r.fields)
      _ <- write(" derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual").when(r.esexprOptions.isDefined)
      _ <- writeln()

      _ <- write("object ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeCompanionBaseTypes[ScalaAnnRecord, ScalaAnnRecord.CompanionExtends](dfn.annotations)(_.`type`)
      _ <- writeln(" {")
      _ <- indent()

      _ <- writeJavaAdapters(dfn)(
        writeToJava = adapterWriter =>
          for
            _ <- write("new ")
            _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- writeln("(")
            _ <- indent()
            _ <- writeScalaToJavaArgs(adapterWriter)(r.fields)
            _ <- dedent()
            _ <- writeln(")")
          yield (),
        writeFromJava = adapterWriter =>
          for
            _ <- AdapterScalaTypeExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- writeln("(")
            _ <- indent()
            _ <- writeScalaFromJavaArgs(adapterWriter)(r.fields)
            _ <- dedent()
            _ <- writeln(")")
          yield (),
      )

      _ <- writeJSAdapters(dfn)(
        writeToJS = adapterWriter =>
          for
            _ <- write("new ")
            _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- writeln(" {")
            _ <- indent()
            _ <- writeScalaToJSArgs(adapterWriter)(r.fields)
            _ <- dedent()
            _ <- writeln("}")
          yield (),
        writeFromJS = adapterWriter =>
          for
            _ <- AdapterScalaTypeExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- writeln("(")
            _ <- indent()
            _ <- writeScalaFromJSArgs(adapterWriter, write("j_value"))(r.fields)
            _ <- dedent()
            _ <- writeln(")")
          yield (),
      )

      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitEnum(dfn: DefinitionInfo, e: EnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- write("enum ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeTypeParameters(dfn.typeParameters)
      _ <- write(" derives _root_.esexpr.ESExprCodec, _root_.scala.CanEqual").when(e.esexprOptions.isDefined)
      _ <- writeln(" {")
      _ <- indent()

      _ <- ZIO.foreachDiscard(e.cases) { c =>
        for
          _ <- ZIO.foreachDiscard(c.esexprOptions) { esexprOptions =>
            esexprOptions.caseType match {
              case constructor: EsexprEnumCaseType.Constructor =>
                for
                  _ <- write("@_root_.esexpr.constructor(\"")
                  _ <- write(StringEscapeUtils.escapeJava(constructor.name).nn)
                  _ <- writeln("\")")
                yield ()

              case _: EsexprEnumCaseType.InlineValue =>
                writeln("@_root_.esexpr.inlineValue")
            }
          }

          _ <- write("case ")
          _ <- write(convertIdPascal(c.name))
          _ <- writeCaseClassParameters(c.fields)
          _ <- writeln()
        yield ()
      }

      _ <- dedent()
      _ <- writeln("}")

      _ <- write("object ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeCompanionBaseTypes[ScalaAnnEnum, ScalaAnnEnum.CompanionExtends](dfn.annotations)(_.`type`)
      _ <- writeln(" {")
      _ <- indent()

      _ <- writeJavaAdapters(dfn)(
        writeToJava = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("s_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case s_value: ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- AdapterScalaTypeExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln(" =>")
                _ <- indent()

                _ <- write("new ")
                _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- adapterWriter.adaptedExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln("(")
                _ <- indent()
                _ <- writeScalaToJavaArgs(adapterWriter)(c.fields)
                _ <- dedent()
                _ <- writeln(")")

                _ <- dedent()
              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
        writeFromJava = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("j_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case j_value: ")
                _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- adapterWriter.adaptedExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln(" =>")
                _ <- indent()

                _ <- write("new ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- AdapterScalaTypeExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln("(")
                _ <- indent()
                _ <- writeScalaFromJavaArgs(adapterWriter)(c.fields)
                _ <- dedent()
                _ <- writeln(")")

                _ <- dedent()
              yield ()
            }

            _ <- writeln("case _ => throw new _root_.scala.MatchError(j_value)")

            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
      )

      _ <- writeJSAdapters(dfn)(
        writeToJS = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("s_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case s_value: ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- AdapterScalaTypeExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln(" =>")
                _ <- indent()

                _ <- write("new ")
                _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- adapterWriter.adaptedExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln(" {")
                _ <- indent()
                _ <- write("override val $type: \"")
                _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
                _ <- write("\" = \"")
                _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
                _ <- writeln("\"")
                _ <- writeScalaToJSArgs(adapterWriter)(c.fields)
                _ <- dedent()
                _ <- writeln("}")

                _ <- dedent()
              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
        writeFromJS = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("j_value.$type match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case \"")
                _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
                _ <- writeln("\" =>")
                _ <- indent()

                _ <- write("new ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- AdapterScalaTypeExprWriter.writeTypeArguments(dfnType.args)
                _ <- writeln("(")
                _ <- indent()
                _ <- writeScalaFromJSArgs(
                  adapterWriter,
                  for
                    _ <- write("j_value.asInstanceOf[")
                    _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                    _ <- write(".")
                    _ <- write(convertIdPascal(c.name))
                    _ <- adapterWriter.adaptedExprWriter.writeTypeArguments(dfnType.args)
                    _ <- write("]")
                  yield ()
                )(c.fields)
                _ <- dedent()
                _ <- writeln(")")

                _ <- dedent()
              yield ()
            }

            _ <- writeln("case _ => throw new _root_.scala.MatchError(\"Unexpected enum case: \" + j_value)")

            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
      )

      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitSimpleEnum(dfn: DefinitionInfo, e: SimpleEnumDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- writeln("@_root_.esexpr.simple").when(e.esexprOptions.isDefined)
      _ <- write("enum ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- write(" derives ")
      _ <- write("_root_.esexpr.ESExprCodec, ").when(e.esexprOptions.isDefined)
      _ <- write("_root_.scala.CanEqual")
      _ <- writeln(" {")
      _ <- indent()

      _ <- ZIO.foreachDiscard(e.cases) { c =>
        for
          _ <- ZIO.foreachDiscard(c.esexprOptions) { esexprOptions =>
            for
              _ <- write("@_root_.esexpr.constructor(\"")
              _ <- write(StringEscapeUtils.escapeJava(esexprOptions.name).nn)
              _ <- writeln("\")")
            yield ()
          }

          _ <- write("case ")
          _ <- write(convertIdPascal(c.name))
          _ <- writeln()
        yield ()
      }

      _ <- dedent()
      _ <- writeln("}")

      _ <- write("object ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeCompanionBaseTypes[ScalaAnnSimpleEnum, ScalaAnnSimpleEnum.CompanionExtends](dfn.annotations)(_.`type`)
      _ <- writeln(" {")
      _ <- indent()

      _ <- writeJavaAdapters(dfn)(
        writeToJava = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("s_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- writeln(" =>")
                _ <- indent()

                _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- writeln(convertIdConst(c.name))

                _ <- dedent()
              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
        writeFromJava = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- write("j_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case _: ")
                _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- writeln(convertIdConst(c.name))
                _ <- writeln(".type =>")
                _ <- indent()

                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- writeln(convertIdPascal(c.name))

                _ <- dedent()
              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
      )

      _ <- writeJSAdapters(dfn)(
        writeToJS = _ => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- writeln("s_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case ")
                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- write(convertIdPascal(c.name))
                _ <- write(" => \"")
                _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
                _ <- writeln("\"")

              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
        writeFromJS = _ => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- write("j_value match {")
            _ <- indent()
            _ <- ZIO.foreachDiscard(e.cases) { c =>
              for
                _ <- write("case \"")
                _ <- write(StringEscapeUtils.escapeJava(c.name).nn)
                _ <- writeln("\" =>")
                _ <- indent()

                _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(dfnType.name)
                _ <- write(".")
                _ <- writeln(convertIdPascal(c.name))

                _ <- dedent()
              yield ()
            }
            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
      )

      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitInterface(dfn: DefinitionInfo, iface: InterfaceDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- write("trait ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeTypeParameters(dfn.typeParameters)
      _ <- writeln(" {")
      _ <- indent()

      _ <- ZIO.foreachDiscard(iface.methods) { m =>
        for
          _ <- write("def ")
          _ <- write(convertIdCamel(m.name))
          _ <- writeTypeParameters(m.typeParameters, constraintType = ConstraintType.ScalaMethod)
          _ <- write("(")

          _ <- ZIO.foreachDiscard(m.parameters.view.zipWithIndex) { (param, index) =>
            for
              _ <- write(", ").when(index > 0)
              _ <- write(convertIdCamel(param.name))
              _ <- write(": ")
              _ <- writeTypeExpr(param.parameterType)
            yield ()
          }

          _ <- write("): _root_.zio.")

          _ <- m.throws match {
            case Some(throwsClause) => write("IO[") *> writeTypeExpr(throwsClause) *> write(", ")
            case _: None.type => write("UIO[")
          }

          _ <- writeTypeExpr(m.returnType)
          _ <- writeln("]")
        yield ()
      }

      _ <- dedent()
      _ <- writeln("}")

      _ <- write("object ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeCompanionBaseTypes[ScalaAnnInterface, ScalaAnnInterface.CompanionExtends](dfn.annotations)(_.`type`)
      _ <- writeln(" {")
      _ <- indent()

      _ <- writeJavaAdapters(dfn)(
        writeToJava = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- write("new ")
            _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
            _ <- writeln(" {")
            _ <- indent()

            _ <- ZIO.foreachDiscard(iface.methods) { m =>
              def writeMethodCall: ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
                for
                  _ <- write("s_value.")
                  _ <- write(convertIdCamel(m.name))
                  _ <- write("(")
                  _ <- ZIO.foreachDiscard(m.parameters.view.zipWithIndex) { (param, index) =>
                    for
                      _ <- write(", ").when(index > 0)
                      _ <- adapterWriter.writeAdapterExpr(param.parameterType, TypePosition.Normal)
                      _ <- write(".fromJava(param_")
                      _ <- write(convertIdCamelNoEscape(param.name))
                      _ <- write(")")
                    yield ()
                  }
                  _ <- write(")")
                yield ()

              for
                _ <- write("override def ")
                _ <- write(convertIdJava(m.name))
                _ <- writeJavaMethodTypeParameters(dfn.typeParameters, m.typeParameters)
                _ <- write("(")

                parameterWriters = Seq(
                  dfn.typeParameters.view
                    .filter {
                      case tp: TypeParameter.Type => tp.constraints.contains(TypeParameterTypeConstraint.Exception())
                    }
                    .map {
                      case tp: TypeParameter.Type =>
                        for
                          _ <- write("errorType_")
                          _ <- write(convertIdCamelNoEscape(tp.name))
                          _ <- write(": _root_.dev.argon.nobleidl.runtime.ErrorType[J")
                          _ <- write(convertIdPascal(tp.name))
                          _ <- write(", ? <: E")
                          _ <- write(convertIdPascal(tp.name))
                          _ <- write("]")
                        yield ()
                    },
                  
                  m.typeParameters.view
                    .filter {
                      case tp: TypeParameter.Type => tp.constraints.contains(TypeParameterTypeConstraint.Exception())
                    }
                    .map {
                      case tp: TypeParameter.Type =>
                        for
                          _ <- write("errorType_")
                          _ <- write(convertIdCamelNoEscape(tp.name))
                          _ <- write(": _root_.dev.argon.nobleidl.runtime.ErrorType[T")
                          _ <- write(convertIdPascal(tp.name))
                          _ <- write(", ? <: E")
                          _ <- write(convertIdPascal(tp.name))
                          _ <- write("]")
                        yield ()
                    },

                  m.parameters.view.map { param =>
                    for
                      _ <- write("param_")
                      _ <- write(convertIdCamelNoEscape(param.name))
                      _ <- write(": ")
                      _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(param.parameterType, TypePosition.Normal)
                    yield ()
                  }
                ).view.flatten

                _ <- ZIO.foreachDiscard(parameterWriters.zipWithIndex) { (paramWriter, index) =>
                  write(", ").when(index > 0) *> paramWriter
                }
                _ <- write("): ")
                _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)
                _ <- writeln(" = {")
                _ <- indent()

                _ <- ZIO.foreachDiscard(
                  m.typeParameters.view
                    .filter {
                      case tp: TypeParameter.Type => tp.constraints.contains(TypeParameterTypeConstraint.Exception())
                    }
                ) {
                  case tp: TypeParameter.Type =>
                    val tpt = TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByMethod)
                    for
                      _ <- write("given _root_.nobleidl.core.ErrorType[")
                      _ <- AdapterScalaTypeExprWriter.writeTypeExpr(tpt, TypePosition.TypeArgument)
                      _ <- write("] = _root_.nobleidl.core.ErrorType.fromJavaErrorType[")
                      _ <- AdapterScalaTypeExprWriter.writeTypeExpr(tpt, TypePosition.TypeArgument)
                      _ <- write("](")
                      _ <- write("errorType_")
                      _ <- write(convertIdCamelNoEscape(tp.name))
                      _ <- writeln(")")
                    yield ()
                }

                _ <- write("_root_.nobleidl.core.JavaIOInterop.runScala[")
                
                
                _ <- ZIO.foreachDiscard(m.throws) { throwsClause =>
                  AdapterScalaTypeExprWriter.writeTypeExpr(throwsClause, TypePosition.Normal) *>
                    write(", ") *>
                    adapterWriter.adaptedExprWriter.writeTypeExpr(throwsClause, TypePosition.Normal) *>
                    write(", ")
                }

                _ <- AdapterScalaTypeExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)
                _ <- write(", ")
                _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)


                _ <- write("](")


                _ <- ZIO.foreachDiscard(m.throws) { throwsClause =>
                  adapterWriter.writeAdapterExpr(throwsClause, TypePosition.Normal) *>
                    write(", ")
                }
                _ <- adapterWriter.writeAdapterExpr(m.returnType, TypePosition.ReturnType)
                _ <- ZIO.foreachDiscard(m.throws) { throwsClause =>
                  
                  def writeJavaErrorType(t: TypeExpr): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
                    t match {
                      case t: TypeExpr.DefinedType =>
                        for
                          _ <- write("_root_.dev.argon.nobleidl.runtime.ErrorType.fromClass(classOf[")
                          _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(t, TypePosition.Normal)
                          _ <- write("])")
                        yield ()

                      case t: TypeExpr.TypeParameter =>
                        for
                          _ <- write("errorType_")
                          _ <- write(convertIdCamelNoEscape(t.name))
                        yield ()
                    }
                  
                  write(", ") *> writeJavaErrorType(throwsClause)
                }

                _ <- write(")(")
                _ <- writeMethodCall
                _ <- writeln(")")

                _ <- dedent()
                _ <- writeln("}")
              yield ()
            }

            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
        writeFromJava = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- write("new ")
            _ <- AdapterScalaTypeExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
            _ <- writeln(" {")
            _ <- indent()

            _ <- ZIO.foreachDiscard(iface.methods) { m =>
              def writeMethodCall: ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
                for
                  _ <- write("j_value.")
                  _ <- write(convertIdJava(m.name))
                  _ <- write("(")

                  argWriters = Seq(
                    dfn.typeParameters.view
                      .filter {
                        case tp: TypeParameter.Type => tp.constraints.contains(TypeParameterTypeConstraint.Exception())
                      }
                      .map {
                        case tp: TypeParameter.Type =>
                          for
                            _ <- write("_root_.nobleidl.core.ErrorType.toJavaErrorType(summon[_root_.nobleidl.core.ErrorType[")
                            _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByType), TypePosition.TypeArgument)
                            _ <- write("]])")
                          yield ()
                      },
                    
                    m.typeParameters.view
                      .filter {
                        case tp: TypeParameter.Type => tp.constraints.contains(TypeParameterTypeConstraint.Exception())
                      }
                      .map {
                        case tp: TypeParameter.Type =>
                          for
                            _ <- write("_root_.nobleidl.core.ErrorType.toJavaErrorType(summon[_root_.nobleidl.core.ErrorType[")
                            _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByMethod), TypePosition.TypeArgument)
                            _ <- write("]])")
                          yield ()
                      },


                    m.parameters.view.map { param =>
                      for
                        _ <- adapterWriter.writeAdapterExpr(param.parameterType, TypePosition.Normal)
                        _ <- write(".toJava(param_")
                        _ <- write(convertIdCamelNoEscape(param.name))
                        _ <- write(")")
                      yield ()
                    },
                  ).view.flatten

                  _ <- ZIO.foreachDiscard(argWriters.zipWithIndex) { (argWriter, index) =>
                    write(", ").when(index > 0) *> argWriter
                  }
                  _ <- writeln(").nn")
                yield ()


              for
                _ <- write("override def ")
                _ <- write(convertIdCamel(m.name))
                _ <- writeTypeParameters(m.typeParameters, prefix = "T", constraintType = ConstraintType.ScalaMethod)
                _ <- write("(")
                _ <- ZIO.foreachDiscard(m.parameters.view.zipWithIndex) { (param, index) =>
                  for
                    _ <- write(", ").when(index > 0)
                    _ <- write("param_")
                    _ <- write(convertIdCamelNoEscape(param.name))
                    _ <- write(": ")
                    _ <- AdapterScalaTypeExprWriter.writeTypeExpr(param.parameterType, TypePosition.Normal)
                  yield ()
                }
                _ <- write("): _root_.zio.")
                _ <- m.throws match {
                  case Some(throwsClause) => write("IO[") *> AdapterScalaTypeExprWriter.writeTypeExpr(throwsClause, TypePosition.Normal) *> write(", ")
                  case _: None.type => write("UIO[")
                }

                _ <- AdapterScalaTypeExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)
                _ <- writeln("] = {")
                _ <- indent()

                _ <- write("_root_.nobleidl.core.JavaIOInterop.runJava[")
                _ <- ZIO.foreachDiscard(m.throws) { throwsClause =>
                  AdapterScalaTypeExprWriter.writeTypeExpr(throwsClause, TypePosition.Normal) *>
                    write(", ") *>
                    adapterWriter.adaptedExprWriter.writeTypeExpr(throwsClause, TypePosition.Normal) *>
                    write(", ")
                }

                _ <- AdapterScalaTypeExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)
                _ <- write(", ")
                _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)
                _ <- write("](")

                _ <- ZIO.foreachDiscard(m.throws) { throwsClause =>
                  adapterWriter.writeAdapterExpr(throwsClause, TypePosition.Normal) *>
                    write(", ")
                }
                _ <- adapterWriter.writeAdapterExpr(m.returnType, TypePosition.ReturnType)

                _ <- ZIO.foreachDiscard(m.throws) {
                  case throwsClause @ TypeExpr.DefinedType(name, _) =>
                    write(", _root_.nobleidl.core.ErrorType.fromJavaErrorType(_root_.dev.argon.nobleidl.runtime.ErrorType.fromClass(_root_.scala.Predef.classOf[") *>
                      adapterWriter.adaptedExprWriter.writeDefinedType(throwsClause, TypePosition.Normal) *>
                      write("]))")

                  case TypeExpr.TypeParameter(name, TypeParameterOwner.ByType) =>
                    write(", summon[_root_.nobleidl.core.ErrorType[J") *>
                      write(convertIdPascal(name)) *>
                      write("]]")

                  case TypeExpr.TypeParameter(name, TypeParameterOwner.ByMethod) =>
                    write(", summon[_root_.nobleidl.core.ErrorType[T") *>
                      write(convertIdPascal(name)) *>
                      write("]]")
                }
                
                _ <- writeln(") {")
                _ <- indent()

                _ <- writeMethodCall

                _ <- dedent()
                _ <- writeln("}")

                _ <- dedent()
                _ <- writeln("}")
              yield ()
            }

            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
      )

      _ <- writeJSAdapters(dfn)(
        writeToJS = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- write("new ")
            _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
            _ <- writeln(" {")
            _ <- indent()

            _ <- ZIO.foreachDiscard(iface.methods) { m =>
              def writeMethodCall: ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
                for
                  _ <- write("s_value.")
                  _ <- write(convertIdCamel(m.name))
                  _ <- write("(")
                  _ <- ZIO.foreachDiscard(m.parameters.view.zipWithIndex) { (param, index) =>
                    for
                      _ <- write(", ").when(index > 0)
                      _ <- adapterWriter.writeAdapterExpr(param.parameterType, TypePosition.Normal)
                      _ <- write(".fromJS(param_")
                      _ <- write(convertIdCamelNoEscape(param.name))
                      _ <- write(")")
                    yield ()
                  }
                  _ <- write(")")
                yield ()

              for
                _ <- write("@_root_.scala.scalajs.js.annotation.JSName(\"")
                _ <- write(StringEscapeUtils.escapeJava(convertIdCamelNoEscape(m.name)))
                _ <- writeln("\")")
                _ <- write("override def ")
                _ <- write(convertIdJava(m.name))
                _ <- writeTypeParameters(m.typeParameters, prefix = "T", constraintType = ConstraintType.ScalaJSMethod)
                _ <- write("(")

                parameterWriters = Seq(
                  m.typeParameters.view
                    .filter {
                      case tp: TypeParameter.Type => tp.constraints.contains(TypeParameterTypeConstraint.Exception())
                    }
                    .map {
                      case tp: TypeParameter.Type =>
                        for
                          _ <- write("errorType_")
                          _ <- write(convertIdCamelNoEscape(tp.name))
                          _ <- write(": _root_.nobleidl.sjs.core.ErrorChecker[")
                          _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByMethod), TypePosition.ReturnType)
                          _ <- write("]")
                        yield ()
                    },

                  m.parameters.view.map { param =>
                    for
                      _ <- write("param_")
                      _ <- write(convertIdCamelNoEscape(param.name))
                      _ <- write(": ")
                      _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(param.parameterType, TypePosition.Normal)
                    yield ()
                  }
                ).view.flatten

                _ <- ZIO.foreachDiscard(parameterWriters.zipWithIndex) { (paramWriter, index) =>
                  write(", ").when(index > 0) *> paramWriter
                }
                _ <- write("): _root_.scala.scalajs.js.Promise[")
                _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)
                _ <- writeln("] = {")
                _ <- indent()

                _ <- ZIO.foreachDiscard(
                  m.typeParameters.view
                    .filter {
                      case tp: TypeParameter.Type => tp.constraints.contains(TypeParameterTypeConstraint.Exception())
                    }
                ) {
                  case tp: TypeParameter.Type =>
                    val tpt = TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByMethod)
                    for
                      _ <- write("given _root_.nobleidl.core.ErrorType[")
                      _ <- AdapterScalaTypeExprWriter.writeTypeExpr(tpt, TypePosition.TypeArgument)
                      _ <- write("] = _root_.nobleidl.core.ErrorType.fromJSErrorChecker[")
                      _ <- AdapterScalaTypeExprWriter.writeTypeExpr(tpt, TypePosition.TypeArgument)
                      _ <- write("](")
                      _ <- write("errorType_")
                      _ <- write(convertIdCamelNoEscape(tp.name))
                      _ <- writeln(")")
                    yield ()
                }

                _ <- write("_root_.nobleidl.core.JSIOInterop.runScala[")


                _ <- ZIO.foreachDiscard(m.throws) { throwsClause =>
                  AdapterScalaTypeExprWriter.writeTypeExpr(throwsClause, TypePosition.Normal) *>
                    write(", ") *>
                    adapterWriter.adaptedExprWriter.writeTypeExpr(throwsClause, TypePosition.Normal) *>
                    write(", ")
                }

                _ <- AdapterScalaTypeExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)
                _ <- write(", ")
                _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)


                _ <- write("](")


                _ <- ZIO.foreachDiscard(m.throws) { throwsClause =>
                  adapterWriter.writeAdapterExpr(throwsClause, TypePosition.Normal) *>
                    write(", ")
                }
                _ <- adapterWriter.writeAdapterExpr(m.returnType, TypePosition.ReturnType)


                _ <- write(")(")
                _ <- writeMethodCall
                _ <- writeln(")")

                _ <- dedent()
                _ <- writeln("}")
              yield ()
            }

            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },

        writeFromJS = adapterWriter => {
          val dfnType = definitionAsType(dfn)
          for
            _ <- write("new ")
            _ <- AdapterScalaTypeExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
            _ <- writeln(" {")
            _ <- indent()

            _ <- ZIO.foreachDiscard(iface.methods) { m =>
              def writeMethodCall: ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
                for
                  _ <- write("j_value.")
                  _ <- write(convertIdCamel(m.name))
                  _ <- write("(")

                  argWriters = Seq(
                    m.typeParameters.view
                      .filter {
                        case tp: TypeParameter.Type => tp.constraints.contains(TypeParameterTypeConstraint.Exception())
                      }
                      .map {
                        case tp: TypeParameter.Type =>
                          for
                            _ <- write("_root_.nobleidl.core.ErrorType.toJSErrorChecker(summon[_root_.nobleidl.core.ErrorType[")
                            _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByMethod), TypePosition.ReturnType)
                            _ <- write("]])")
                          yield ()
                      },


                    m.parameters.view.map { param =>
                      for
                        _ <- adapterWriter.writeAdapterExpr(param.parameterType, TypePosition.Normal)
                        _ <- write(".toJS(param_")
                        _ <- write(convertIdCamelNoEscape(param.name))
                        _ <- write(")")
                      yield ()
                    },
                  ).view.flatten

                  _ <- ZIO.foreachDiscard(argWriters.zipWithIndex) { (argWriter, index) =>
                    write(", ").when(index > 0) *> argWriter
                  }
                  _ <- writeln(").nn")
                yield ()


              for
                _ <- write("override def ")
                _ <- write(convertIdCamel(m.name))
                _ <- writeTypeParameters(m.typeParameters, prefix = "T", constraintType = ConstraintType.ScalaMethod)
                _ <- write("(")
                _ <- ZIO.foreachDiscard(m.parameters.view.zipWithIndex) { (param, index) =>
                  for
                    _ <- write(", ").when(index > 0)
                    _ <- write("param_")
                    _ <- write(convertIdCamelNoEscape(param.name))
                    _ <- write(": ")
                    _ <- AdapterScalaTypeExprWriter.writeTypeExpr(param.parameterType, TypePosition.Normal)
                  yield ()
                }
                _ <- write("): _root_.zio.")
                _ <- m.throws match {
                  case Some(throwsClause) => write("IO[") *> AdapterScalaTypeExprWriter.writeTypeExpr(throwsClause, TypePosition.Normal) *> write(", ")
                  case _: None.type => write("UIO[")
                }

                _ <- AdapterScalaTypeExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)
                _ <- writeln("] = {")
                _ <- indent()

                _ <- write("_root_.nobleidl.core.JSIOInterop.runJS[")
                _ <- ZIO.foreachDiscard(m.throws) { throwsClause =>
                  AdapterScalaTypeExprWriter.writeTypeExpr(throwsClause, TypePosition.Normal) *>
                    write(", ") *>
                    adapterWriter.adaptedExprWriter.writeTypeExpr(throwsClause, TypePosition.Normal) *>
                    write(", ")
                }

                _ <- AdapterScalaTypeExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)
                _ <- write(", ")
                _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(m.returnType, TypePosition.ReturnType)
                _ <- write("](")

                _ <- ZIO.foreachDiscard(m.throws) { throwsClause =>
                  adapterWriter.writeAdapterExpr(throwsClause, TypePosition.Normal) *>
                    write(", ")
                }
                _ <- adapterWriter.writeAdapterExpr(m.returnType, TypePosition.ReturnType)

                _ <- ZIO.foreachDiscard(m.throws) {
                  case throwsClause @ TypeExpr.DefinedType(name, _) =>
                    write(", _root_.nobleidl.core.ErrorType.fromJSErrorChecker(") *>
                      adapterWriter.adaptedExprWriter.writeDefinedType(throwsClause, TypePosition.Normal) *>
                      write(".errorChecker)")

                  case TypeExpr.TypeParameter(name, TypeParameterOwner.ByType) =>
                    write(", summon[_root_.nobleidl.core.ErrorType[J") *>
                      write(convertIdPascal(name)) *>
                      write("]]")

                  case TypeExpr.TypeParameter(name, TypeParameterOwner.ByMethod) =>
                    write(", summon[_root_.nobleidl.core.ErrorType[T") *>
                      write(convertIdPascal(name)) *>
                      write("]]")
                }

                _ <- writeln(") {")
                _ <- indent()

                _ <- writeMethodCall

                _ <- dedent()
                _ <- writeln("}")

                _ <- dedent()
                _ <- writeln("}")
              yield ()
            }

            _ <- dedent()
            _ <- writeln("}")
          yield ()
        },
      )

      _ <- dedent()
      _ <- writeln("}")

    yield ()

  protected override def emitExceptionType(dfn: DefinitionInfo, ex: ExceptionTypeDefinition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- write("package ")
      _ <- getScalaPackage(dfn.name.`package`).flatMap(writeln)

      _ <- write("class ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeln("(")
      _ <- indent()
      _ <- write("val information: ")
      _ <- writeTypeExpr(ex.information)
      _ <- writeln(",")
      _ <- writeln("message: _root_.java.lang.String | _root_.scala.Null = null,")
      _ <- writeln("cause: _root_.java.lang.Throwable | _root_.scala.Null = null,")
      _ <- dedent()
      _ <- writeln(") extends _root_.java.lang.Exception(message, cause)")

      _ <- write("object ")
      _ <- write(convertIdPascal(dfn.name.name))
      _ <- writeCompanionBaseTypes[ScalaAnnException, ScalaAnnException.CompanionExtends](dfn.annotations)(_.`type`)
      _ <- writeln(" {")
      _ <- indent()

      dfnType = definitionAsType(dfn)

      _ <- writeJavaAdapters(dfn)(
        writeToJava = adapterWriter => {
          for
            _ <- write("val ex = new ")
            _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- write("(")
            _ <- adapterWriter.writeAdapterExpr(ex.information, TypePosition.Normal)
            _ <- writeln(".toJava(s_value.information), s_value.getMessage, s_value.getCause)")
            _ <- writeln("ex.setStackTrace(s_value.getStackTrace)")
            _ <- writeln("ex")
          yield ()
        },

        writeFromJava = adapterWriter => {
          for
            _ <- write("val ex = new ")
            _ <- AdapterScalaTypeExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- write("(")
            _ <- adapterWriter.writeAdapterExpr(ex.information, TypePosition.Normal)
            _ <- writeln(".fromJava(j_value.information), j_value.getMessage, j_value.getCause)")
            _ <- writeln("ex.setStackTrace(j_value.getStackTrace)")
            _ <- writeln("ex")
          yield ()
        }
      )

      _ <- writeJSAdapters(dfn)(
        writeToJS = adapterWriter => {
          for
            _ <- adapterWriter.adaptedExprWriter.writeDefinedTypeName(dfn.name)
            _ <- write(".createError(")
            _ <- indent()
            _ <- adapterWriter.writeAdapterExpr(ex.information, TypePosition.Normal)
            _ <- writeln(".toJS(s_value.information),")
            _ <- writeln("s_value.getMessage match { case msg if _root_.java.util.Objects.isNull(msg) => _root_.scala.scalajs.js.undefined case msg => msg },")
            _ <- writeln("s_value.getCause match { case c if _root_.java.util.Objects.isNull(c) => _root_.scala.scalajs.js.undefined case c => new _root_.nobleidl.sjs.core.ErrorOptions { override val cause: _root_.scala.Any = c } },")
            _ <- dedent()
            _ <- writeln(")")
          yield ()
        },

        writeFromJS = adapterWriter => {
          for
            _ <- write("new ")
            _ <- AdapterScalaTypeExprWriter.writeTypeExpr(definitionAsType(dfn), TypePosition.Normal)
            _ <- write("(")
            _ <- indent()
            _ <- adapterWriter.writeAdapterExpr(ex.information, TypePosition.Normal)
            _ <- writeln(".fromJS(j_value.information),")
            _ <- writeln("_root_.scala.Some(j_value.message).filter(_.nonEmpty).orNull,")
            _ <- writeln("j_value.asInstanceOf[_root_.scala.scalajs.js.Dictionary[_root_.scala.Matchable]].get(\"cause\").map { case ex: _root_.java.lang.Throwable => ex case obj => _root_.scala.scalajs.js.JavaScriptException(obj) }.orNull,")
            _ <- dedent()
            _ <- writeln(")")
          yield ()
        },
      )

      _ <- dedent()
      _ <- writeln("}")
    yield ()


  private def writeCaseClassParameters(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- writeln("(")
      _ <- indent()

      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- ZIO.foreachDiscard(field.esexprOptions) { esexprOptions =>
            esexprOptions.kind match {
              case positional: EsexprRecordFieldKind.Positional =>
                positional.mode match {
                  case EsexprRecordPositionalMode.Required() => ZIO.unit
                  case EsexprRecordPositionalMode.Optional(_) => writeln("@_root_.esexpr.optional")
                }


              case keyword: EsexprRecordFieldKind.Keyword =>
                for
                  _ <- write("@_root_.esexpr.keyword(\"")
                  _ <- write(StringEscapeUtils.escapeJava(keyword.name).nn)
                  _ <- writeln("\")")

                  _ <- keyword.mode match {
                    case EsexprRecordKeywordMode.Required() => ZIO.unit

                    case EsexprRecordKeywordMode.Optional(_) =>
                      writeln("@_root_.esexpr.optional")

                    case EsexprRecordKeywordMode.DefaultValue(_) => ZIO.unit
                  }
                yield ()

              case _: EsexprRecordFieldKind.Dict =>
                writeln("@_root_.esexpr.dict")

              case _: EsexprRecordFieldKind.Vararg =>
                writeln("@_root_.esexpr.vararg")
            }
          }

          _ <- write(convertIdCamel(field.name))
          _ <- write(": ")
          _ <- writeTypeExpr(field.fieldType)
          _ <- ZIO.foreachDiscard(field.esexprOptions) { esexprOptions =>
            esexprOptions.kind match {
              case EsexprRecordFieldKind.Keyword(_, EsexprRecordKeywordMode.DefaultValue(defaultValue)) =>
                write(" = ") *> writeDecodedValue(defaultValue)

              case _ => ZIO.unit
            }
          }

          _ <- writeln(",")
        yield ()
      }

      _ <- dedent()
      _ <- write(")")
    yield ()


  private def writeScalaToJavaArgs(adapterWriter: AdapterWriter)(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- adapterWriter.writeAdapterExpr(field.fieldType, TypePosition.Normal)
          _ <- write(".toJava(s_value.")
          _ <- write(convertIdCamel(field.name))
          _ <- writeln("),")
        yield ()
      }
    yield ()

  private def writeScalaFromJavaArgs(adapterWriter: AdapterWriter)(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- adapterWriter.writeAdapterExpr(field.fieldType, TypePosition.Normal)
          _ <- write(".fromJava(j_value.")
          _ <- write(convertIdJava(field.name))
          _ <- writeln("().nn),")
        yield ()
      }
    yield ()

  private def writeScalaToJSArgs(adapterWriter: AdapterWriter)(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- write("override val ")
          _ <- write(convertIdCamel(field.name))
          _ <- write(": ")
          _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(field.fieldType, TypePosition.Normal)
          _ <- write(" = ")
          _ <- adapterWriter.writeAdapterExpr(field.fieldType, TypePosition.Normal)
          _ <- write(".toJS(s_value.")
          _ <- write(convertIdCamel(field.name))
          _ <- writeln(")")
        yield ()
      }
    yield ()

  private def writeScalaFromJSArgs(adapterWriter: AdapterWriter, writeValue: ZIO[CodeWriter, NobleIDLCompileErrorException, Unit])(fields: Seq[RecordField]): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    for
      _ <- ZIO.foreachDiscard(fields) { field =>
        for
          _ <- adapterWriter.writeAdapterExpr(field.fieldType, TypePosition.Normal)
          _ <- write(".fromJS(")
          _ <- writeValue
          _ <- write(".")
          _ <- write(convertIdCamel(field.name))
          _ <- writeln("),")
        yield ()
      }
    yield ()

  private def writeCompanionBaseTypes[T <: Matchable, C <: T]
  (anns: Seq[api.Annotation])
  (using TypeTest[T, C], ESExprCodec[T])
  (getType: C => String)
  : ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    ZStream.fromIterable(anns)
        .filter(_.scope == "scala")
        .mapZIO(ann => ZIO.fromEither(summon[ESExprCodec[T]].decode(ann.value)))
        .mapError(ex => NobleIDLCompileErrorException("Could not decode scala scoped annotation", ex))
        .collect {
          case c: C => c
        }
        .zipWithIndex
        .foreach { (ann, index) =>
          val extendKeyword = if index > 0 then " with " else " extends "
          write(extendKeyword) *> write(getType(ann))
        }



  private def writeDecodedValue(value: EsexprDecodedValue): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    value match {
      case EsexprDecodedValue.Record(t, fields) =>
        for
          _ <- writeTypeExpr(t)
          _ <- write("(")
          _ <- ZIO.foreachDiscard(fields.view.zipWithIndex) { (field, index) =>
            write(", ").when(index > 0) *> writeDecodedValue(field.value)
          }
          _ <- write(")")
        yield ()

      case EsexprDecodedValue.Enum(t, caseName, fields) =>
        for
          t <- t match {
            case t: TypeExpr.DefinedType => ZIO.succeed(t)
            case _ => ZIO.fail(NobleIDLCompileErrorException("Expected a defined type for an enum"))
          }

          _ <- ScalaTypeExprWriter.writeDefinedTypeCase(t, caseName, TypePosition.Normal)
          _ <- write("(")
          _ <- ZIO.foreachDiscard(fields.view.zipWithIndex) { (field, index) =>
            write(", ").when(index > 0) *> writeDecodedValue(field.value)
          }
          _ <- write(")")
        yield ()

      case EsexprDecodedValue.SimpleEnum(t, caseName) =>
        for
          t <- t match {
            case t: TypeExpr.DefinedType => ZIO.succeed(t)
            case _ => ZIO.fail(NobleIDLCompileErrorException("Expected a defined type for a simple enum"))
          }

          _ <- ScalaTypeExprWriter.writeDefinedTypeCase(t, caseName, TypePosition.Normal)
        yield ()

      case EsexprDecodedValue.Optional(t, elementType, value) =>
        for
          _ <- writeStaticMethod(t, "fromOptional")
          _ <- write("(")

          _ <- value match {
            case Some(value) =>
              for
                _ <- write("_root_.scala.Some[")
                _ <- writeTypeExpr(elementType)
                _ <- write("](")
                _ <- writeDecodedValue(value)
                _ <- write(")")
              yield ()

            case _: None.type =>
              for
                _ <- write("_root_.scala.Option.empty[")
                _ <- writeTypeExpr(elementType)
                _ <- write("]")
              yield ()
          }

          _ <- write(")")
        yield ()

      case EsexprDecodedValue.Vararg(t, elementType, values) =>
        for
          _ <- writeStaticMethod(t, "fromSeq")
          _ <- write("(_root_.scala.collection.immutable.Seq[")
          _ <- writeTypeExpr(elementType)
          _ <- write("](")

          _ <- ZIO.foreachDiscard(values.view.zipWithIndex) { (value, index) =>
            write(", ").when(index > 0) *> writeDecodedValue(value)
          }

          _ <- write("))")
        yield ()

      case EsexprDecodedValue.Dict(t, elementType, values) =>
        for
          _ <- writeStaticMethod(t, "fromMap")
          _ <- write("(_root_.scala.collection.immutable.Map[_root_.java.lang.String, ")
          _ <- writeTypeExpr(elementType)
          _ <- write("](")

          _ <- ZIO.foreachDiscard(values.dict.view.zipWithIndex) {
            case ((key, value), index) =>
              for
                _ <- write(", ").whenDiscard(index > 0)
                _ <- write("(\"")
                _ <- write(StringEscapeUtils.escapeJava(key).nn)
                _ <- write("\", ")
                _ <- writeDecodedValue(value)
                _ <- write(")")
              yield ()
          }

          _ <- write("))")
        yield ()

      case EsexprDecodedValue.BuildFrom(t, _, fromValue) =>
        for
          _ <- writeStaticMethod(t, "buildFrom")
          _ <- write("(")
          _ <- writeDecodedValue(fromValue)
          _ <- write(")")
        yield ()

      case EsexprDecodedValue.FromBool(t, b) =>
        for
          _ <- writeStaticMethod(t, "fromBoolean")
          _ <- write("(")
          _ <- write(b.toString)
          _ <- write(")")
        yield ()

      case EsexprDecodedValue.FromInt(t, value, minValue, maxValue) =>
        def writeSignedIntValue(typeName: String, value: String): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
          for
            _ <- writeStaticMethod(t, "from" + typeName)
            _ <- write("(")
            _ <- write(value)
            _ <- write(")")
          yield ()

        def writeUnsignedIntValue(typeName: String, value: String): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
          for
            _ <- writeStaticMethod(t, "from" + typeName)
            _ <- write("(_root_.esexpr.unsigned.to")
            _ <- write(typeName)
            _ <- write("(")
            _ <- write(value)
            _ <- write("))")
          yield ()

        (minValue, maxValue) match {
          case (Some(minValue), Some(maxValue)) if minValue >= UByte.MinValue.toBigInt && maxValue <= UByte.MaxValue.toBigInt =>
            writeUnsignedIntValue("UByte", value.byteValue.toString)

          case (Some(minValue), Some(maxValue)) if minValue >= UShort.MinValue.toBigInt && maxValue <= UShort.MaxValue.toBigInt =>
            writeUnsignedIntValue("UShort", value.shortValue.toString)

          case (Some(minValue), Some(maxValue)) if minValue >= UInt.MinValue.toBigInt && maxValue <= UInt.MaxValue.toBigInt =>
            writeUnsignedIntValue("UInt", value.intValue.toString)

          case (Some(minValue), Some(maxValue)) if minValue >= ULong.MinValue.toBigInt && maxValue <= ULong.MaxValue.toBigInt =>
            writeUnsignedIntValue("ULong", value.longValue.toString + "L")

          case (Some(minValue), Some(maxValue)) if minValue >= Byte.MinValue && maxValue <= Byte.MaxValue =>
            writeSignedIntValue("Byte", value.byteValue.toString)

          case (Some(minValue), Some(maxValue)) if minValue >= Short.MinValue && maxValue <= Short.MaxValue =>
            writeSignedIntValue("Short", value.shortValue.toString)

          case (Some(minValue), Some(maxValue)) if minValue >= Int.MinValue && maxValue <= Int.MaxValue =>
            writeSignedIntValue("Int", value.intValue.toString)

          case (Some(minValue), Some(maxValue)) if minValue >= Long.MinValue && maxValue <= Long.MaxValue =>
            writeSignedIntValue("Long", value.longValue.toString + "L")

          case _ =>
            for
              _ <- writeStaticMethod(t, "fromBigInt")
              _ <- write("(_root_.scala.math.BigInt(\"")
              _ <- write(value.toString)
              _ <- write("\"))")
            yield ()
        }

      case EsexprDecodedValue.FromStr(t, s) =>
        for
          _ <- writeStaticMethod(t, "fromString")
          _ <- write("(\"")
          _ <- write(StringEscapeUtils.escapeJava(s).nn)
          _ <- write("\")")
        yield ()

      case EsexprDecodedValue.FromBinary(t, b) =>
        for
          _ <- writeStaticMethod(t, "fromBinary")
          _ <- write("(_root_.scala.IArray[_root_.scala.Byte](")
          _ <- ZIO.foreachDiscard(b.array.view.zipWithIndex) { (value, index) =>
            write(", ").when(index > 0) *> write(value.toString)
          }
          _ <- write("))")
        yield ()

      case EsexprDecodedValue.FromFloat32(t, f) =>
        for
          _ <- writeStaticMethod(t, "fromFloat")
          _ <- write("(")
          _ <- write(
            if f.isNaN then
              "_root_.scala.Float.NaN"
            else if f.isPosInfinity then
              "_root_.scala.Float.PositiveInfinity"
            else if f.isNegInfinity then
              "_root_.scala.Float.NegativeInfinity"
            else
              s"_root_.java.lang.Float.intBitsToFloat(${ java.lang.Float.floatToRawIntBits(f) })"
          )
          _ <- write(")")
        yield ()

      case EsexprDecodedValue.FromFloat64(t, f) =>
        for
          _ <- writeStaticMethod(t, "fromDouble")
          _ <- write("(")
          _ <- write(
            if f.isNaN then
              "_root_.scala.Double.NaN"
            else if f.isPosInfinity then
              "_root_.scala.Double.PositiveInfinity"
            else if f.isNegInfinity then
              "_root_.scala.Double.NegativeInfinity"
            else
              s"_root_.java.lang.Double.longBitsToDouble(${ java.lang.Double.doubleToRawLongBits(f) })"
          )
          _ <- write(")")
        yield ()


      case EsexprDecodedValue.FromNull(t, level, Some(maxLevel)) if maxLevel == 0 =>
        for
          _ <- writeStaticMethod(t, "fromNull")
        yield ()

      case EsexprDecodedValue.FromNull(t, level, _) =>
        for
          _ <- writeStaticMethod(t, "fromNull")
          _ <- write("(")
          _ <- write(level.fold("0")(_.toString))
          _ <- write(")")
        yield ()
    }

  private def writeStaticMethod(t: TypeExpr, methodName: String): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    t match {
      case TypeExpr.DefinedType(name, args) =>
        for
          _ <- ScalaTypeExprWriter.writeDefinedTypeName(name)
          _ <- write(".")
          _ <- write(methodName)
          _ <- ScalaTypeExprWriter.writeTypeArguments(args)
        yield ()

      case TypeExpr.TypeParameter(_, _) =>
        ZIO.fail(NobleIDLCompileErrorException("Cannot call static method of a type parameter"))
    }

  private def writeJavaAdapters(
    dfn: DefinitionInfo,
  )(
    writeToJava: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
    writeFromJava: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
  ): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    writePlatformAdapters(dfn)(
      adapterMethod = "javaAdapter",
      toMethod = "toJava",
      fromMethod = "fromJava",
    )(
      buildAdapterWriter = () => options.javaAdapters.map { javaAdapters =>
        JavaAdapterWriter(JavaTypeExprWriter(buildPackageMapping(javaAdapters.packageMapping.mapping.dict)))
      },
      writeToPlatform = writeToJava,
      writeFromPlatform = writeFromJava,
    )

  private def writeJSAdapters(dfn: DefinitionInfo)(
    writeToJS: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
    writeFromJS: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
  ): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    writePlatformAdapters(dfn)(
      adapterMethod = "jsAdapter",
      toMethod = "toJS",
      fromMethod = "fromJS",
    )(
      buildAdapterWriter = () => options.jsAdapters.map { jsAdapters =>
        JSAdapterWriter(JSTypeExprWriter(buildPackageMapping(jsAdapters.packageMapping.mapping.dict)))
      },
      writeToPlatform = writeToJS,
      writeFromPlatform = writeFromJS,
    )


  private def writePlatformAdapters(dfn: DefinitionInfo)(
    adapterMethod: String,
    toMethod: String,
    fromMethod: String,
  )(
    buildAdapterWriter: () => Option[AdapterWriter],
    writeToPlatform: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
    writeFromPlatform: AdapterWriter => ZIO[CodeWriter, NobleIDLCompileErrorException, Unit],
  ): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
    ZIO.foreachDiscard(buildAdapterWriter()) { adapterWriter =>
      val dfnType = definitionAsType(dfn)

      for
        _ <- write("def ")
        _ <- write(adapterMethod)
        _ <- adapterWriter.writeTypeParamPairs(dfn)

        _ <- write("(")
        _ <- ZIO.foreachDiscard(dfn.typeParameters.view.zipWithIndex) {
          case (tp: TypeParameter.Type, index) =>
            for
              _ <- write(", ").when(index > 0)
              _ <- write(convertIdCamelNoEscape(tp.name))
              _ <- write("Adapter: ")
              _ <- adapterWriter.writeAdapterType(TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByType), TypePosition.Normal)
            yield ()
        }
        _ <- write(")")

        givenWriters = Seq(
          dfn.typeParameters.view
            .flatMap {
              case tp: TypeParameter.Type if tp.constraints.contains(TypeParameterTypeConstraint.Exception()) =>
                Seq(
                  for
                    _ <- write("_root_.nobleidl.core.ErrorType[")
                    _ <- AdapterScalaTypeExprWriter.writeTypeExpr(TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByType), TypePosition.Normal)
                    _ <- write("], _root_.nobleidl.core.ErrorType[")
                    _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(TypeExpr.TypeParameter(tp.name, TypeParameterOwner.ByType), TypePosition.Normal)
                    _ <- write("]")
                  yield ()
                )

              case _ => Seq.empty
            },

          if adapterNeedsZioRuntime(TypeExpr.DefinedType(dfn.name, Seq()), Set()) then
            Seq(
              write("_root_.zio.Runtime[_root_.scala.Any]"),
            )
          else
            Seq(),
        ).view.flatten

        _ <- (
          write("(using ") *>
            ZIO.foreachDiscard(givenWriters.zipWithIndex) { (writeAction, index) =>
              for
                _ <- write(", ").when(index > 0)
                _ <- writeAction
              yield ()
            } *>
            write(")")
        ).when(givenWriters.nonEmpty)

        _ <- write(": ")
        _ <- adapterWriter.writeAdapterType(dfnType, TypePosition.Normal)
        _ <- writeln(" =")
        _ <- indent()
        _ <- write("new ")
        _ <- adapterWriter.writeAdapterType(dfnType, TypePosition.Normal)
        _ <- writeln(" {")
        _ <- indent()

        _ <- write("override def ")
        _ <- write(toMethod)
        _ <- write("(s_value: ")
        _ <- AdapterScalaTypeExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
        _ <- write("): ")
        _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
        _ <- writeln(" = {")
        _ <- indent()
        _ <- writeToPlatform(adapterWriter)
        _ <- dedent()
        _ <- writeln("}")

        _ <- write("override def ")
        _ <- write(fromMethod)
        _ <- write("(j_value: ")
        _ <- adapterWriter.adaptedExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
        _ <- write("): ")
        _ <- AdapterScalaTypeExprWriter.writeTypeExpr(dfnType, TypePosition.Normal)
        _ <- writeln(" = {")
        _ <- indent()
        _ <- writeFromPlatform(adapterWriter)
        _ <- dedent()
        _ <- writeln("}")

        _ <- dedent()
        _ <- writeln("}")
        _ <- dedent()
      yield ()
    }


  private def adapterNeedsZioRuntime(t: TypeExpr, seenTypes: Set[QualifiedName]): Boolean =
    t match {
      case TypeExpr.DefinedType(name, args) =>
        val needsRuntime =
          if seenTypes.contains(name) then
            false
          else
            model.definitions
              .find(_.name == name)
              .exists { dfn =>
                dfn.definition match {
                  case Definition.Record(r) =>
                    r.fields.exists { f =>
                      adapterNeedsZioRuntime(f.fieldType, seenTypes + name)
                    }

                  case Definition.Enum(e) =>
                    e.cases.exists { c =>
                      c.fields.exists { f =>
                        adapterNeedsZioRuntime(f.fieldType, seenTypes + name)
                      }
                    }

                  case Definition.SimpleEnum(_) => false
                  case Definition.ExternType(_) =>
                    dfn.annotations
                      .view
                      .filter(_.scope == "scala")
                      .flatMap { ann => summon[ESExprCodec[ScalaLanguageExternTypeAnnotations]].decode(ann.value).toOption }
                      .exists {
                        case ScalaLanguageExternTypeAnnotations.AdapterNeedsZioRuntime => true
                      }

                  case Definition.Interface(_) => true
                  case Definition.ExceptionType(ex) => adapterNeedsZioRuntime(ex.information, seenTypes + name)
                }
              }

        needsRuntime || args.exists(adapterNeedsZioRuntime(_, seenTypes))

      case _: TypeExpr.TypeParameter => false
    }

  
  private def convertIdJava(kebab: String): String =
    escapeIdentifier(dev.argon.nobleidl.compiler.JavaBackendUtils.convertIdCamel(kebab))
  

  private object AdapterScalaTypeExprWriter extends TypeExprWriter {
    override def writeTypeParameter(parameter: TypeExpr.TypeParameter, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      parameter.owner match {
        case TypeParameterOwner.ByType => write("S" + convertIdPascal(parameter.name))
        case TypeParameterOwner.ByMethod => write("T" + convertIdPascal(parameter.name))
      }

  }

  private abstract class AdapterWriter {

    def adapterType: String
    def adapterMemberName: String
    def adaptedExprWriter: TypeExprWriter

    def writeAdapterType(t: TypeExpr, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      for
        _ <- write("_root_.nobleidl.core.")
        _ <- write(adapterType)
        _ <- write("[")
        _ <- AdapterScalaTypeExprWriter.writeTypeExpr(t, pos)
        _ <- write(", ")
        _ <- adaptedExprWriter.writeTypeExpr(t, pos)
        _ <- write("]")
      yield ()

    def writeAdapterExpr(t: TypeExpr, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      t match {
        case TypeExpr.DefinedType(name, args) =>
          for
            _ <- AdapterScalaTypeExprWriter.writeDefinedTypeName(name)
            _ <- write(".")
            _ <- write(adapterMemberName)
            _ <- writeTypeArgPairs(args)
            _ <- write("(")
            _ <- ZIO.foreachDiscard(args.view.zipWithIndex) { (arg, index) =>
              for
                _ <- write(", ").when(index > 0)
                _ <- writeAdapterExpr(arg, TypePosition.TypeArgument)
              yield ()
            }
            _ <- write(")")
          yield ()

        case TypeExpr.TypeParameter(name, TypeParameterOwner.ByType) =>
          write(convertIdCamelNoEscape(name) + "Adapter")

        case TypeExpr.TypeParameter(_, TypeParameterOwner.ByMethod) =>
          writeIdentityAdapter(t)
      }

    def writeIdentityAdapter(t: TypeExpr): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]

    def writeTypeParamPairs(dfn: DefinitionInfo): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit]

    def writeTypeArgPairs(args: Seq[TypeExpr]) =
      (
        for
          _ <- write("[")
          _ <- ZIO.foreachDiscard(args.view.zipWithIndex) { (arg, index) =>
            for
              _ <- write(", ").when(index > 0)
              _ <- AdapterScalaTypeExprWriter.writeTypeExpr(arg, TypePosition.TypeArgument)
              _ <- write(", ")
              _ <- adaptedExprWriter.writeTypeExpr(arg, TypePosition.TypeArgument)
            yield ()
          }
          _ <- write("]")
        yield ()
        ).whenDiscard(args.nonEmpty)
  }

  private abstract class JavaAdapterWriterBase extends AdapterWriter {
    override def adapterType: String = "JavaAdapter"
    override def adapterMemberName: String = "javaAdapter"
    override def adaptedExprWriter: JavaTypeExprWriter

    override def writeTypeParamPairs(dfn: DefinitionInfo): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      (
        for
          _ <- write("[")
          _ <- ZIO.foreachDiscard(dfn.typeParameters.view.zipWithIndex) {
            case (param: TypeParameter.Type, index) =>
              for
                _ <- write(", ").when(index > 0)
                _ <- write("S")
                _ <- write(convertIdPascal(param.name))
                _ <- write(", J")
                _ <- write(convertIdPascal(param.name))
              yield ()
          }
          _ <- write("]")
        yield ()
        ).whenDiscard(dfn.typeParameters.nonEmpty)

    override def writeIdentityAdapter(t: TypeExpr): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      write(s"_root_.nobleidl.core.JavaAdapter.identity[") *>
        AdapterScalaTypeExprWriter.writeTypeExpr(t, TypePosition.TypeArgument) *>
        write("]")
  }

  private class JavaAdapterWriterBoxed(val adaptedExprWriter: JavaTypeExprWriter) extends JavaAdapterWriterBase {
    override def adapterType: String = "JavaAdapter"
    override def adapterMemberName: String = "javaAdapterBoxed"
  }

  private class JavaAdapterWriter(val adaptedExprWriter: JavaTypeExprWriter) extends JavaAdapterWriterBase() {
    override def adapterType: String = "JavaAdapter"
    override def adapterMemberName: String = "javaAdapter"

    override def writeAdapterExpr(t: TypeExpr, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      if adaptedExprWriter.isBoxedType(t, pos) then
        JavaAdapterWriterBoxed(adaptedExprWriter).writeAdapterExpr(t, pos)
      else
        super.writeAdapterExpr(t, pos)
  }

  private class JavaTypeExprWriter(javaPackageMapping: Map[PackageName, String]) extends TypeExprWriter {
    override def writeDefinedType(definedType: TypeExpr.DefinedType, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      getMappedType(definedType)
        .map(writeMappedType(_, _, pos))
        .getOrElse(super.writeDefinedType(definedType, pos))

    private def getMappedType(t: TypeExpr): Option[(JavaMappedType, Map[String, TypeExpr])] =
      Some(t)
        .collect {
          case t: TypeExpr.DefinedType => t
        }
        .flatMap { definedType =>
          model.definitions.find(_.name == definedType.name)
            .flatMap { dfn =>
                dfn.definition match {
                  case Definition.ExternType(_) =>
                    dfn.annotations
                      .view
                      .filter(_.scope == "java")
                      .flatMap { ann =>
                        summon[ESExprCodec[JavaAnnExternType]].decode(ann.value).toOption
                      }
                      .collectFirst {
                        case JavaAnnExternType.MappedTo(mappedType) => mappedType
                      }
                      .map { mappedType =>
                        val argMapping = dfn.typeParameters
                          .map { case tp: TypeParameter.Type => tp.name }
                          .zip(definedType.args)
                          .toMap

                        (mappedType, argMapping)
                      }

                  case _ => None
                }
              }
        }

    private def getJavaPackage(packageName: PackageName): IO[NobleIDLCompileErrorException, String] =
      ZIO.fromEither(
        javaPackageMapping.get(packageName)
          .toRight { NobleIDLCompileErrorException("Unmapped Java package: " + packageName.display) }
      )

    override def writePackageName(packageName: PackageName): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      getJavaPackage(packageName).flatMap(write)

    override def writeTypeParameter(parameter: TypeExpr.TypeParameter, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      parameter.owner match {
        case TypeParameterOwner.ByType => write("J" + convertIdPascal(parameter.name))
        case TypeParameterOwner.ByMethod => write("T" + convertIdPascal(parameter.name))
      }

    private def writeMappedType(t: JavaMappedType, argsMapping: Map[String, TypeExpr], pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      val boxed = pos == TypePosition.TypeArgument
      t match {
        case JavaMappedType.TypeName("boolean") => write(if boxed then "_root_.java.lang.Boolean" else "_root_.scala.Boolean")
        case JavaMappedType.TypeName("char") => write(if boxed then "_root_.java.lang.Character" else "_root_.scala.Char")
        case JavaMappedType.TypeName("byte") => write(if boxed then "_root_.java.lang.Byte" else "_root_.scala.Byte")
        case JavaMappedType.TypeName("short") => write(if boxed then "_root_.java.lang.Short" else "_root_.scala.Short")
        case JavaMappedType.TypeName("int") => write(if boxed then "_root_.java.lang.Integer" else "_root_.scala.Int")
        case JavaMappedType.TypeName("long") => write(if boxed then "_root_.java.lang.Long" else "_root_.scala.Long")
        case JavaMappedType.TypeName("float") => write(if boxed then "_root_.java.lang.Float" else "_root_.scala.Float")
        case JavaMappedType.TypeName("double") => write(if boxed then "_root_.java.lang.Double" else "_root_.scala.Double")
        case JavaMappedType.TypeName("void") => write(if pos == TypePosition.ReturnType then "_root_.scala.Unit" else "_root_.dev.argon.nobleidl.runtime.Unit")
        case JavaMappedType.TypeName(name) => write("_root_.") *> write(name)
        case JavaMappedType.Apply(name, args) =>
          for
            _ <- write("_root_.")
            _ <- write(name)
            _ <- write("[")
            _ <- ZIO.foreachDiscard(args.view.zipWithIndex) { (arg, index) =>
              write(", ").when(index > 0) *> writeMappedType(arg, argsMapping, TypePosition.TypeArgument)
            }
            _ <- write("]")
          yield ()

        case JavaMappedType.Annotated(t, _) => writeMappedType(t, argsMapping, pos)

        case JavaMappedType.TypeParameter(name) =>
          ZIO.fromEither(argsMapping.get(name).toRight { NobleIDLCompileErrorException("Unknown type parameter: " + name) })
            .flatMap(writeTypeExpr(_, pos))

        case JavaMappedType.Array(elementType) =>
          write("_root_.scala.Array[") *> writeMappedType(elementType, argsMapping, TypePosition.Normal) *> write("]")
      }
    end writeMappedType

    def isBoxedType(t: TypeExpr, pos: TypePosition): Boolean =
      getMappedType(t).exists { (mappedType, _) =>
        def checkMappedType(mappedType: JavaMappedType): Boolean =
          mappedType match {
            case JavaMappedType.TypeName("boolean" | "char" | "byte" | "short" | "int" | "long" | "float" | "double") =>
              pos == TypePosition.TypeArgument

            case JavaMappedType.TypeName("void") => pos != TypePosition.ReturnType

            case JavaMappedType.Annotated(inner, _) => checkMappedType(inner)

            case _ => false
          }

        checkMappedType(mappedType)
      }
  }


  private class JSAdapterWriter(val adaptedExprWriter: JSTypeExprWriter) extends AdapterWriter {
    override def adapterType: String = "JSAdapter"
    override def adapterMemberName: String = "jsAdapter"

    override def writeTypeParamPairs(dfn: DefinitionInfo): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      (
        for
          _ <- write("[")
          _ <- ZIO.foreachDiscard(dfn.typeParameters.view.zipWithIndex) {
            case (param: TypeParameter.Type, index) =>
              for
                _ <- write(", ").when(index > 0)
                _ <- write("S")
                _ <- write(convertIdPascal(param.name))
                _ <- write(", J")
                _ <- write(convertIdPascal(param.name))
              yield ()
          }
          _ <- write("]")
        yield ()
        ).whenDiscard(dfn.typeParameters.nonEmpty)

    override def writeIdentityAdapter(t: TypeExpr): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      write(s"_root_.nobleidl.core.JSAdapter.identity[") *>
        AdapterScalaTypeExprWriter.writeTypeExpr(t, TypePosition.TypeArgument) *>
        write("]")
  }

  private class JSTypeExprWriter(jsPackageMapping: Map[PackageName, String]) extends TypeExprWriter {

    private def getJSPackage(packageName: PackageName): IO[NobleIDLCompileErrorException, String] =
      ZIO.fromEither(
        jsPackageMapping.get(packageName)
          .toRight {
            NobleIDLCompileErrorException("Unmapped JS package: " + packageName.display)
          }
      )

    override def writePackageName(packageName: PackageName): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      getJSPackage(packageName).flatMap(write)

    override def writeTypeParameter(parameter: TypeExpr.TypeParameter, pos: TypePosition): ZIO[CodeWriter, NobleIDLCompileErrorException, Unit] =
      parameter.owner match {
        case TypeParameterOwner.ByType => write("J" + convertIdPascal(parameter.name))
        case TypeParameterOwner.ByMethod => write("T" + convertIdPascal(parameter.name))
      }
  }

}
