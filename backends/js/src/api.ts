import * as esexpr from "@argon-lang/esexpr";
import { ESExpr, type ESExprCodec } from "@argon-lang/esexpr";


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
            errors: esexpr.varargFieldCodec(esexpr.arrayRepeatedValuesCodec(esexpr.strCodec)),
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
            parts: esexpr.varargFieldCodec(esexpr.arrayRepeatedValuesCodec(esexpr.strCodec)),
        },
    );

	export function isSamePackage(a: PackageName, b: PackageName): boolean {
		if(a.parts.length !== b.parts.length) {
			return false;
		}

		for(let i = 0; i < a.parts.length; ++i) {
			if(a.parts[i] !== b.parts[i]) {
				return false;
			}
		}

		return true;
	}
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
            fields: esexpr.varargFieldCodec(esexpr.arrayRepeatedValuesCodec(RecordField.codec)),
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
            cases: esexpr.varargFieldCodec(esexpr.arrayRepeatedValuesCodec(EnumCase.codec)),
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
            fields: esexpr.varargFieldCodec(esexpr.arrayRepeatedValuesCodec(RecordField.codec)),
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
            methods: esexpr.varargFieldCodec(esexpr.arrayRepeatedValuesCodec(InterfaceMethod.codec)),
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
    export const codec: ESExprCodec<TypeExpr> = esexpr.lazyCodec(() => esexpr.enumCodec({
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




export type ESExprAnnRecord =
	| { readonly $type: "derive-codec" }
	| { readonly $type: "constructor", readonly name: string }
;

export namespace ESExprAnnRecord {
	export const codec: ESExprCodec<ESExprAnnRecord> = esexpr.enumCodec({
		"derive-codec": esexpr.caseCodec({}),
		constructor: esexpr.caseCodec({
			name: esexpr.positionalFieldCodec(esexpr.strCodec),
		}),
	});
}

export type ESExprAnnEnum =
	| { readonly $type: "derive-codec" }
	| { readonly $type: "simple-enum" }
;

export namespace ESExprAnnEnum {
	export const codec: ESExprCodec<ESExprAnnEnum> = esexpr.enumCodec({
		"derive-codec": esexpr.caseCodec({}),
		"simple-enum": esexpr.caseCodec({}),
	});
}

export type ESExprAnnEnumCase =
	| { readonly $type: "constructor", readonly name: string }
	| { readonly $type: "inline-value" }
;

export namespace ESExprAnnEnumCase {
	export const codec: ESExprCodec<ESExprAnnEnumCase> = esexpr.enumCodec({
		constructor: esexpr.caseCodec({
			name: esexpr.positionalFieldCodec(esexpr.strCodec),
		}),
		"inline-value": esexpr.caseCodec({}),
	});
}

export type ESExprAnnRecordField =
	| {
		readonly $type: "keyword",
		readonly name?: string | undefined,
		readonly required: boolean,
		readonly defaultValue?: ESExpr | undefined,
	}
	| { readonly $type: "dict" }
	| { readonly $type: "vararg" }
;

export namespace ESExprAnnRecordField {
	export const codec: ESExprCodec<ESExprAnnRecordField> = esexpr.enumCodec({
		keyword: esexpr.caseCodec({
			name: esexpr.optionalKeywordFieldCodec("name", esexpr.undefinedOptionalCodec(esexpr.strCodec)),
			required: esexpr.defaultKeywordFieldCodec("required", () => true, esexpr.boolCodec),
			defaultValue: esexpr.optionalKeywordFieldCodec("default-value", esexpr.undefinedOptionalCodec(ESExpr.codec)),
		}),
		dict: esexpr.caseCodec({}),
		vararg: esexpr.caseCodec({}),
	});
}

export type ESExprAnnExternType =
	| { readonly $type: "derive-codec" }
	| { readonly $type: "allow-optional" }
	| { readonly $type: "allow-vararg" }
	| { readonly $type: "allow-dict" }
	| {
		readonly $type: "literals"
		readonly literals: ESExprAnnExternTypeLiterals,
	}
;

export namespace ESExprAnnExternType {
	export const codec: ESExprCodec<ESExprAnnExternType> = esexpr.lazyCodec(() => esexpr.enumCodec({
		"derive-codec": esexpr.caseCodec({}),
		"allow-optional": esexpr.caseCodec({}),
		"allow-vararg": esexpr.caseCodec({}),
		"allow-dict": esexpr.caseCodec({}),
		literals: esexpr.inlineCaseCodec("literals", ESExprAnnExternTypeLiterals.codec),
	}));
}

export interface ESExprAnnExternTypeLiterals {
	readonly allowBool: boolean,
	readonly allowInt: boolean,
	readonly minInt?: bigint | undefined,
	readonly maxInt?: bigint | undefined,
	readonly allowStr: boolean,
	readonly allowBinary: boolean,
	readonly allowFloat32: boolean,
	readonly allowFloat64: boolean,
	readonly allowNull: boolean,
	readonly buildLiteralFrom?: QualifiedName | undefined,
}

export namespace ESExprAnnExternTypeLiterals {
	export const codec: ESExprCodec<ESExprAnnExternTypeLiterals> = esexpr.lazyCodec(() => esexpr.recordCodec("literals", {
		allowBool: esexpr.defaultKeywordFieldCodec("allow-bool", () => false, esexpr.boolCodec),
		allowInt: esexpr.defaultKeywordFieldCodec("allow-int", () => false, esexpr.boolCodec),
		minInt: esexpr.optionalKeywordFieldCodec("min-int", esexpr.undefinedOptionalCodec(esexpr.intCodec)),
		maxInt: esexpr.optionalKeywordFieldCodec("max-int", esexpr.undefinedOptionalCodec(esexpr.intCodec)),
		allowStr: esexpr.defaultKeywordFieldCodec("allow-str", () => false, esexpr.boolCodec),
		allowBinary: esexpr.defaultKeywordFieldCodec("allow-binary", () => false, esexpr.boolCodec),
		allowFloat32: esexpr.defaultKeywordFieldCodec("allow-float32", () => false, esexpr.boolCodec),
		allowFloat64: esexpr.defaultKeywordFieldCodec("allow-float64", () => false, esexpr.boolCodec),
		allowNull: esexpr.defaultKeywordFieldCodec("allow-binary", () => false, esexpr.boolCodec),
		buildLiteralFrom: esexpr.optionalKeywordFieldCodec("build-literal-from", esexpr.undefinedOptionalCodec(QualifiedName.codec)),
	}));
}

