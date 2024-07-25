import * as esexpr from "@argon-lang/esexpr";
import type { ESExpr, ESExprCodec } from "@argon-lang/esexpr";


export interface NobleIDLGenerationRequest<L> {
    readonly languageOptions: L,
    readonly model: NobleIDLModel,
}

export namespace NobleIDLGenerationRequest {
    export function codec<L>(lCodec: ESExprCodec<L>): ESExprCodec<NobleIDLGenerationRequest<L>> {
        return esexpr.lazyCodec(() => esexpr.recordCodec(
            "noble-idl-generation-request",
            {
                languageOptions: esexpr.keywordFieldCodec("language-options", lCodec),
                model: esexpr.keywordFieldCodec("model", NobleIDLModel.codec),
            }
        ));
    }
}

export interface NobleIDLGenerationResult {
    readonly generatedFiles: readonly string[],
}

export namespace NobleIDLGenerationResult {
    export const codec: ESExprCodec<NobleIDLGenerationResult> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "noble-idl-generation-result",
        {
            generatedFiles: esexpr.keywordFieldCodec("library-files", esexpr.listCodec(esexpr.strCodec)),
        },
    ));
}




export interface NobleIDLCompileModelOptions {
    readonly libraryFiles: readonly string[];
    readonly files: readonly string[];
}

export namespace NobleIDLCompileModelOptions {
    export const codec: ESExprCodec<NobleIDLCompileModelOptions> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "options",
        {
            libraryFiles: esexpr.keywordFieldCodec("library-files", esexpr.listCodec(esexpr.strCodec)),
            files: esexpr.keywordFieldCodec("files", esexpr.listCodec(esexpr.strCodec)),
        },
    ));
}

export type NobleIDLCompileModelResult =
    | { readonly $type: "success", readonly model: NobleIDLModel }
    | { readonly $type: "failure", readonly errors: readonly string[] }
;

export namespace NobleIDLCompileModelResult {
    export const codec: ESExprCodec<NobleIDLCompileModelResult> = esexpr.lazyCodec(() => esexpr.enumCodec<NobleIDLCompileModelResult>({
        success: esexpr.caseCodec({
            model: esexpr.positionalFieldCodec(NobleIDLModel.codec),
        }),
        failure: esexpr.caseCodec({
            errors: esexpr.varargFieldCodec(esexpr.strCodec),
        }),
    }));
}


export interface NobleIDLModel {
    readonly definitions: readonly DefinitionInfo[],
}

export namespace NobleIDLModel {
    export const codec: ESExprCodec<NobleIDLModel> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "noble-idl-model",
        {
            definitions: esexpr.keywordFieldCodec("definitions", esexpr.listCodec(DefinitionInfo.codec)),
        },
    ));
}

export interface DefinitionInfo {
    readonly name: QualifiedName,
    readonly typeParameters: readonly TypeParameter[],
    readonly definition: Definition,
    readonly annotations: readonly Annotation[],
}

export namespace DefinitionInfo {
    export const codec: ESExprCodec<DefinitionInfo> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "definition-info",
        {
            name: esexpr.keywordFieldCodec("name", QualifiedName.codec),
            typeParameters: esexpr.keywordFieldCodec("type-parameters", esexpr.listCodec(TypeParameter.codec)),
            definition: esexpr.keywordFieldCodec("definition", Definition.codec),
            annotations: esexpr.keywordFieldCodec("annotations", esexpr.listCodec(Annotation.codec)),
        },
    ));
}


export interface PackageName {
    readonly parts: readonly string[],
}

export namespace PackageName {
    export const codec: ESExprCodec<PackageName> = esexpr.recordCodec(
        "package-name",
        {
            parts: esexpr.varargFieldCodec(esexpr.strCodec),
        },
    );
}

export interface QualifiedName {
    readonly package: PackageName,
    readonly name: string,
}

export namespace QualifiedName {
    export const codec: ESExprCodec<QualifiedName> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "qualified-name",
        {
            package: esexpr.positionalFieldCodec(PackageName.codec),
            name: esexpr.positionalFieldCodec(esexpr.strCodec),
        },
    ));
}

export type Definition =
    | { readonly $type: "record", readonly record: RecordDefinition }
    | { readonly $type: "enum", readonly enum: EnumDefinition }
    | { readonly $type: "extern-type" }
    | { readonly $type: "interface", readonly interface: InterfaceDefinition }
;

export namespace Definition {
    export const codec: ESExprCodec<Definition> = esexpr.lazyCodec(() => esexpr.enumCodec({
        record: esexpr.inlineCaseCodec("record", RecordDefinition.codec),
        enum: esexpr.inlineCaseCodec("enum", EnumDefinition.codec),
        "extern-type": esexpr.caseCodec({}),
        interface: esexpr.inlineCaseCodec("interface", InterfaceDefinition.codec),
    }));
}

export interface RecordDefinition {
    readonly fields: readonly RecordField[],
}

export namespace RecordDefinition {
    export const codec: ESExprCodec<RecordDefinition> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "record-definition",
        {
            fields: esexpr.varargFieldCodec(RecordField.codec),
        },
    ));
}

export interface RecordField {
    readonly name: string,
    readonly fieldType: TypeExpr,

    readonly annotations: readonly Annotation[],
}

export namespace RecordField {
    export const codec: ESExprCodec<RecordField> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "record-field",
        {
            name: esexpr.positionalFieldCodec(esexpr.strCodec),
            fieldType: esexpr.positionalFieldCodec(TypeExpr.codec),
            annotations: esexpr.keywordFieldCodec("annotations", esexpr.listCodec(Annotation.codec)),
        },
    ))
}

export interface EnumDefinition {
    readonly cases: readonly EnumCase[],
}

export namespace EnumDefinition {
    export const codec: ESExprCodec<EnumDefinition> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "enum-definition",
        {
            cases: esexpr.varargFieldCodec(EnumCase.codec),
        },
    ));
}

export interface EnumCase {
    readonly name: string,
    readonly fields: readonly RecordField[],

    readonly annotations: readonly Annotation[],
}

export namespace EnumCase {
    export const codec: ESExprCodec<EnumCase> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "enum-case",
        {
            name: esexpr.positionalFieldCodec(esexpr.strCodec),
            fields: esexpr.varargFieldCodec(RecordField.codec),
            annotations: esexpr.keywordFieldCodec("annotations", esexpr.listCodec(Annotation.codec)),
        },
    ))
}

export interface InterfaceDefinition {
    readonly methods: readonly InterfaceMethod[],
}

export namespace InterfaceDefinition {
    export const codec: ESExprCodec<InterfaceDefinition> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "interface-definition",
        {
            methods: esexpr.varargFieldCodec(InterfaceMethod.codec),
        },
    ));
}

export interface InterfaceMethod {
    readonly name: string,
    readonly typeParameters: readonly TypeParameter[],
    readonly parameters: readonly InterfaceMethodParameter[],
    readonly returnType: TypeExpr,

    readonly annotations: readonly Annotation[],
}

export namespace InterfaceMethod {
    export const codec: ESExprCodec<InterfaceMethod> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "interface-method",
        {
            name: esexpr.keywordFieldCodec("name", esexpr.strCodec),
            typeParameters: esexpr.keywordFieldCodec("type-parameters", esexpr.listCodec(TypeParameter.codec)),
            parameters: esexpr.keywordFieldCodec("parameters", esexpr.listCodec(InterfaceMethodParameter.codec)),
            returnType: esexpr.keywordFieldCodec("return-type", TypeExpr.codec),
            annotations: esexpr.keywordFieldCodec("annotations", esexpr.listCodec(Annotation.codec)),
        },
    ))
}

export interface InterfaceMethodParameter {
    readonly name: string,
    readonly parameterType: TypeExpr,

    readonly annotations: readonly Annotation[],
}

export namespace InterfaceMethodParameter {
    export const codec: ESExprCodec<InterfaceMethodParameter> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "interface-method-parameter",
        {
            name: esexpr.positionalFieldCodec(esexpr.strCodec),
            parameterType: esexpr.positionalFieldCodec(TypeExpr.codec),
            annotations: esexpr.keywordFieldCodec("annotations", esexpr.listCodec(Annotation.codec)),
        },
    ))
}

export interface Annotation {
    readonly scope: string,
    readonly value: ESExpr,
}

export namespace Annotation {
    export const codec: ESExprCodec<Annotation> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "annotation",
        {
            scope: esexpr.positionalFieldCodec(esexpr.strCodec),
            value: esexpr.positionalFieldCodec(esexpr.ESExpr.codec),
        },
    ));
}

export type TypeExpr =
    | { readonly $type: "defined-type", readonly name: QualifiedName }
    | { readonly $type: "type-parameter", readonly name: string }
    | { readonly $type: "apply", readonly baseType: TypeExpr, readonly args: readonly TypeExpr[] }
;

export namespace TypeExpr {
    export const codec: ESExprCodec<TypeExpr> = esexpr.lazyCodec(() => esexpr.enumCodec<TypeExpr>({
        "defined-type": esexpr.caseCodec({
            name: esexpr.positionalFieldCodec(QualifiedName.codec),
        }),
        "type-parameter": esexpr.caseCodec({
            name: esexpr.positionalFieldCodec(esexpr.strCodec),
        }),
        apply: esexpr.caseCodec({
            baseType: esexpr.positionalFieldCodec(TypeExpr.codec),
            args: esexpr.positionalFieldCodec(esexpr.listCodec(TypeExpr.codec)),
        }),
    }))
}

export type TypeParameter =
    | { readonly $type: "type", readonly name: string }
;

export namespace TypeParameter {
    export const codec: ESExprCodec<TypeParameter> = esexpr.enumCodec({
        type: esexpr.caseCodec({
            name: esexpr.positionalFieldCodec(esexpr.strCodec),
        }),
    });
}


