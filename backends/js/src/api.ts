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
        success: esexpr.caseCodec("success", {
            model: esexpr.positionalFieldCodec(NobleIDLModel.codec),
        }),
        failure: esexpr.caseCodec("failure", {
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
	readonly isLibrary: boolean,
}

export namespace DefinitionInfo {
    export const codec: ESExprCodec<DefinitionInfo> = esexpr.lazyCodec(() => esexpr.recordCodec(
        "definition-info",
        {
            name: esexpr.keywordFieldCodec("name", QualifiedName.codec),
            typeParameters: esexpr.keywordFieldCodec("type-parameters", esexpr.listCodec(TypeParameter.codec)),
            definition: esexpr.keywordFieldCodec("definition", Definition.codec),
            annotations: esexpr.keywordFieldCodec("annotations", esexpr.listCodec(Annotation.codec)),
			isLibrary: esexpr.keywordFieldCodec("is-library", esexpr.boolCodec),
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
    | { readonly $type: "extern-type", readonly ext: ExternTypeDefinition }
    | { readonly $type: "interface", readonly interface: InterfaceDefinition }
;

export namespace Definition {
    export const codec: ESExprCodec<Definition> = esexpr.lazyCodec(() => esexpr.enumCodec<Definition>({
        record: esexpr.inlineCaseCodec("record", RecordDefinition.codec),
        enum: esexpr.inlineCaseCodec("enum", EnumDefinition.codec),
        "extern-type": esexpr.inlineCaseCodec("ext", ExternTypeDefinition.codec),
        interface: esexpr.inlineCaseCodec("interface", InterfaceDefinition.codec),
    }));
}

export interface RecordDefinition {
    readonly fields: readonly RecordField[],
	readonly esexprOptions?: ESExprRecordOptions | undefined,
}

export namespace RecordDefinition {
    export const codec: ESExprCodec<RecordDefinition> = esexpr.lazyCodec(() => esexpr.recordCodec<RecordDefinition>(
        "record-definition",
        {
            fields: esexpr.varargFieldCodec(esexpr.arrayRepeatedValuesCodec(RecordField.codec)),
			esexprOptions: esexpr.optionalKeywordFieldCodec("esexpr-options", esexpr.undefinedOptionalCodec(ESExprRecordOptions.codec)),
        },
    ));
}

export interface RecordField {
    readonly name: string,
    readonly fieldType: TypeExpr,

    readonly annotations: readonly Annotation[],
	readonly esexprOptions?: ESExprRecordFieldOptions | undefined,
}

export namespace RecordField {
    export const codec: ESExprCodec<RecordField> = esexpr.lazyCodec(() => esexpr.recordCodec<RecordField>(
        "record-field",
        {
            name: esexpr.positionalFieldCodec(esexpr.strCodec),
            fieldType: esexpr.positionalFieldCodec(TypeExpr.codec),
            annotations: esexpr.keywordFieldCodec("annotations", esexpr.listCodec(Annotation.codec)),
			esexprOptions: esexpr.optionalKeywordFieldCodec("esexpr-options", esexpr.undefinedOptionalCodec(ESExprRecordFieldOptions.codec)),
        },
    ))
}

export interface EnumDefinition {
    readonly cases: readonly EnumCase[],
	readonly esexprOptions?: ESExprEnumOptions | undefined,
}

export namespace EnumDefinition {
    export const codec: ESExprCodec<EnumDefinition> = esexpr.lazyCodec(() => esexpr.recordCodec<EnumDefinition>(
        "enum-definition",
        {
            cases: esexpr.varargFieldCodec(esexpr.arrayRepeatedValuesCodec(EnumCase.codec)),
			esexprOptions: esexpr.optionalKeywordFieldCodec("esexpr-options", esexpr.undefinedOptionalCodec(ESExprEnumOptions.codec)),
        },
    ));
}

export interface EnumCase {
    readonly name: string,
    readonly fields: readonly RecordField[],

    readonly annotations: readonly Annotation[],


	readonly esexprOptions?: ESExprEnumCaseOptions | undefined,
}

export namespace EnumCase {
    export const codec: ESExprCodec<EnumCase> = esexpr.lazyCodec(() => esexpr.recordCodec<EnumCase>(
        "enum-case",
        {
            name: esexpr.positionalFieldCodec(esexpr.strCodec),
            fields: esexpr.varargFieldCodec(esexpr.arrayRepeatedValuesCodec(RecordField.codec)),
            annotations: esexpr.keywordFieldCodec("annotations", esexpr.listCodec(Annotation.codec)),
			esexprOptions: esexpr.optionalKeywordFieldCodec("esexpr-options", esexpr.undefinedOptionalCodec(ESExprEnumCaseOptions.codec)),
        },
    ));
}

export interface ExternTypeDefinition {
	readonly esexprOptions?: ESExprExternTypeOptions | undefined,
}

export namespace ExternTypeDefinition {
    export const codec: ESExprCodec<ExternTypeDefinition> = esexpr.lazyCodec(() => esexpr.recordCodec<ExternTypeDefinition>(
        "extern-type-definition",
        {
			esexprOptions: esexpr.optionalKeywordFieldCodec("esexpr-options", esexpr.undefinedOptionalCodec(ESExprExternTypeOptions.codec)),
        },
    ));

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
    | { readonly $type: "defined-type", readonly name: QualifiedName, readonly args: readonly TypeExpr[] }
    | { readonly $type: "type-parameter", readonly name: string }
;

export namespace TypeExpr {
    export const codec: ESExprCodec<TypeExpr> = esexpr.lazyCodec(() => esexpr.enumCodec({
        "defined-type": esexpr.caseCodec("defined-type", {
            name: esexpr.positionalFieldCodec(QualifiedName.codec),
            args: esexpr.positionalFieldCodec(esexpr.listCodec(TypeExpr.codec)),
        }),
        "type-parameter": esexpr.caseCodec("type-parameter", {
            name: esexpr.positionalFieldCodec(esexpr.strCodec),
        }),
    }))
}

export type TypeParameter =
    | { readonly $type: "type", readonly name: string }
;

export namespace TypeParameter {
    export const codec: ESExprCodec<TypeParameter> = esexpr.enumCodec({
        type: esexpr.caseCodec("type", {
            name: esexpr.positionalFieldCodec(esexpr.strCodec),
        }),
    });
}


// ESExpr options
export interface ESExprRecordOptions {
	readonly constructor: string,
}

export namespace ESExprRecordOptions {
	export const codec: ESExprCodec<ESExprRecordOptions> = esexpr.recordCodec(
		"record-options",
		{
			constructor: esexpr.positionalFieldCodec(esexpr.strCodec),
		},
	);
}

export interface ESExprEnumOptions {
	readonly simpleEnum: boolean,
}

export namespace ESExprEnumOptions {
	export const codec: ESExprCodec<ESExprEnumOptions> = esexpr.recordCodec(
		"enum-options",
		{
			simpleEnum: esexpr.defaultKeywordFieldCodec("simple-enum", () => false, esexpr.boolCodec),
		},
	);
}

export interface ESExprEnumCaseOptions {
	readonly caseType: ESExprEnumCaseType,
}

export namespace ESExprEnumCaseOptions {
	export const codec: ESExprCodec<ESExprEnumCaseOptions> = esexpr.lazyCodec(() => esexpr.recordCodec<ESExprEnumCaseOptions>(
		"enum-case-options",
		{
			caseType: esexpr.positionalFieldCodec(ESExprEnumCaseType.codec),
		},
	));
}

export type ESExprEnumCaseType =
	| { readonly $type: "constructor", readonly name: string }
	| { readonly $type: "inline-value" }
;

export namespace ESExprEnumCaseType {
	export const codec: ESExprCodec<ESExprEnumCaseType> = esexpr.enumCodec({
		constructor: esexpr.caseCodec("constructor", {
			name: esexpr.positionalFieldCodec(esexpr.strCodec),
		}),
		"inline-value": esexpr.caseCodec("inline-value", {}),
	});
}

export interface ESExprExternTypeOptions {
	allowValue: boolean,
	allowOptional?: TypeExpr | undefined,
	allowVararg?: TypeExpr | undefined,
	allowDict?: TypeExpr | undefined,
	literals: ESExprAnnExternTypeLiterals,
}

export namespace ESExprExternTypeOptions {
	export const codec: ESExprCodec<ESExprExternTypeOptions> = esexpr.lazyCodec(() => esexpr.recordCodec<ESExprExternTypeOptions>(
		"extern-type-options",
		{
			allowValue: esexpr.defaultKeywordFieldCodec("allow-value", () => false, esexpr.boolCodec),
			allowOptional: esexpr.optionalKeywordFieldCodec("allow-optional", esexpr.undefinedOptionalCodec(TypeExpr.codec)),
			allowVararg: esexpr.optionalKeywordFieldCodec("allow-vararg", esexpr.undefinedOptionalCodec(TypeExpr.codec)),
			allowDict: esexpr.optionalKeywordFieldCodec("allow-dict", esexpr.undefinedOptionalCodec(TypeExpr.codec)),
			literals: esexpr.keywordFieldCodec("literals", ESExprAnnExternTypeLiterals.codec),
		},
	));
}

export interface ESExprRecordFieldOptions {
	readonly kind: ESExprRecordFieldKind,
}

export namespace ESExprRecordFieldOptions {
	export const codec: ESExprCodec<ESExprRecordFieldOptions> = esexpr.lazyCodec(() => esexpr.recordCodec<ESExprRecordFieldOptions>(
		"field-options",
		{
			kind: esexpr.positionalFieldCodec(ESExprRecordFieldKind.codec),
		},
	));
}

export type ESExprRecordFieldKind =
	| { readonly $type: "positional", readonly mode: ESExprRecordPositionalMode }
	| { readonly $type: "keyword", readonly name: string, readonly mode: ESExprRecordKeywordMode }
	| { readonly $type: "dict", readonly elementType: TypeExpr }
	| { readonly $type: "vararg", readonly elementType: TypeExpr }
;

export namespace ESExprRecordFieldKind {
	export const codec: ESExprCodec<ESExprRecordFieldKind> = esexpr.lazyCodec(() => esexpr.enumCodec({
		positional: esexpr.caseCodec("positional", {
			mode: esexpr.positionalFieldCodec(ESExprRecordPositionalMode.codec),
		}),
		keyword: esexpr.caseCodec("keyword", {
			name: esexpr.positionalFieldCodec(esexpr.strCodec),
			mode: esexpr.positionalFieldCodec(ESExprRecordKeywordMode.codec),
		}),
		dict: esexpr.caseCodec("dict", {
			elementType: esexpr.positionalFieldCodec(TypeExpr.codec),
		}),
		vararg: esexpr.caseCodec("vararg", {
			elementType: esexpr.positionalFieldCodec(TypeExpr.codec),
		}),
	}));
}

export type ESExprRecordPositionalMode =
	| { readonly $type: "required" }
	| { readonly $type: "optional", readonly elementType: TypeExpr }
;

export namespace ESExprRecordPositionalMode {
	export const codec: ESExprCodec<ESExprRecordPositionalMode> = esexpr.lazyCodec(() => esexpr.enumCodec<ESExprRecordPositionalMode>({
		required: esexpr.caseCodec("required", {}),
		optional: esexpr.caseCodec("optional", {
			elementType: esexpr.positionalFieldCodec(TypeExpr.codec),
		}),
	}));
}

export type ESExprRecordKeywordMode =
	| { readonly $type: "required" }
	| { readonly $type: "optional", readonly elementType: TypeExpr }
	| { readonly $type: "default-value", readonly defaultValue: ESExprDecodedValue }
;

export namespace ESExprRecordKeywordMode {
	export const codec: ESExprCodec<ESExprRecordKeywordMode> = esexpr.lazyCodec(() => esexpr.enumCodec<ESExprRecordKeywordMode>({
		required: esexpr.caseCodec("required", {}),
		optional: esexpr.caseCodec("optional", {
			elementType: esexpr.positionalFieldCodec(TypeExpr.codec),
		}),
		"default-value": esexpr.caseCodec("default-value", {
			defaultValue: esexpr.positionalFieldCodec(ESExprDecodedValue.codec),
		}),
	}));
}

export type ESExprDecodedValue =
	| {
		readonly $type: "record",
		readonly t: TypeExpr,
		readonly fieldValues: ReadonlyMap<string, ESExprDecodedValue>,
	}
	| {
		readonly $type: "enum",
		readonly t: TypeExpr,
		readonly caseName: string,
		readonly fieldValues: ReadonlyMap<string, ESExprDecodedValue>,
	}
	| {
		readonly $type: "optional",
		readonly t: TypeExpr,
		readonly value?: ESExprDecodedValue | undefined,
	}
	| {
		readonly $type: "vararg",
		readonly t: TypeExpr,
		readonly values: readonly ESExprDecodedValue[],
	}
	| {
		readonly $type: "dict",
		readonly t: TypeExpr,
		readonly values: ReadonlyMap<string, ESExprDecodedValue>,
	}
	| {
		readonly $type: "build-from",
		readonly t: TypeExpr,
		readonly value: ESExprDecodedValue,
	}
	| {
		readonly $type: "from-bool",
		readonly t: TypeExpr,
		readonly b: boolean,
	}
	| {
		readonly $type: "from-int",
		readonly t: TypeExpr,
		readonly i: bigint,
		readonly minInt?: bigint | undefined,
		readonly maxInt?: bigint | undefined,
	}
	| {
		readonly $type: "from-str",
		readonly t: TypeExpr,
		readonly s: string,
	}
	| {
		readonly $type: "from-binary",
		readonly t: TypeExpr,
		readonly b: Uint8Array,
	}
	| {
		readonly $type: "from-float32",
		readonly t: TypeExpr,
		readonly f: number,
	}
	| {
		readonly $type: "from-float64",
		readonly t: TypeExpr,
		readonly f: number,
	}
	| {
		readonly $type: "from-null",
		readonly t: TypeExpr,
	}
;

export namespace ESExprDecodedValue {
	export const codec: ESExprCodec<ESExprDecodedValue> = esexpr.lazyCodec(() => esexpr.enumCodec<ESExprDecodedValue>({
		record: esexpr.caseCodec("record", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			fieldValues: esexpr.dictFieldCodec(esexpr.mapMappedValueCodec(ESExprDecodedValue.codec)),
		}),
		enum: esexpr.caseCodec("enum", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			caseName: esexpr.positionalFieldCodec(esexpr.strCodec),
			fieldValues: esexpr.dictFieldCodec(esexpr.mapMappedValueCodec(ESExprDecodedValue.codec)),
		}),
		optional: esexpr.caseCodec("optional", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			value: esexpr.optionalPositionalFieldCodec(esexpr.undefinedOptionalCodec(ESExprDecodedValue.codec)),
		}),
		vararg: esexpr.caseCodec("vararg", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			values: esexpr.varargFieldCodec(esexpr.arrayRepeatedValuesCodec(ESExprDecodedValue.codec)),
		}),
		dict: esexpr.caseCodec("dict", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			values: esexpr.dictFieldCodec(esexpr.mapMappedValueCodec(ESExprDecodedValue.codec)),
		}),
		"build-from": esexpr.caseCodec("build-from", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			value: esexpr.positionalFieldCodec(ESExprDecodedValue.codec),
		}),
		"from-bool": esexpr.caseCodec("from-bool", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			b: esexpr.positionalFieldCodec(esexpr.boolCodec),
		}),
		"from-int": esexpr.caseCodec("from-int", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			i: esexpr.positionalFieldCodec(esexpr.intCodec),
			minInt: esexpr.optionalKeywordFieldCodec("min-int", esexpr.undefinedOptionalCodec(esexpr.intCodec)),
			maxInt: esexpr.optionalKeywordFieldCodec("max-int", esexpr.undefinedOptionalCodec(esexpr.intCodec)),
		}),
		"from-str": esexpr.caseCodec("from-str", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			s: esexpr.positionalFieldCodec(esexpr.strCodec),
		}),
		"from-binary": esexpr.caseCodec("from-binary", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			b: esexpr.positionalFieldCodec(esexpr.binaryCodec),
		}),
		"from-float32": esexpr.caseCodec("from-float32", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			f: esexpr.positionalFieldCodec(esexpr.float32Codec),
		}),
		"from-float64": esexpr.caseCodec("from-float64", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
			f: esexpr.positionalFieldCodec(esexpr.float64Codec),
		}),
		"from-null": esexpr.caseCodec("from-null", {
			t: esexpr.positionalFieldCodec(TypeExpr.codec),
		}),
	}));
}



// ESExpr annotations
export type ESExprAnnRecord =
	| { readonly $type: "derive-codec" }
	| { readonly $type: "constructor", readonly name: string }
;

export namespace ESExprAnnRecord {
	export const codec: ESExprCodec<ESExprAnnRecord> = esexpr.enumCodec({
		"derive-codec": esexpr.caseCodec("derive-codec", {}),
		constructor: esexpr.caseCodec("constructor", {
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
		"derive-codec": esexpr.caseCodec("derive-codec", {}),
		"simple-enum": esexpr.caseCodec("simple-enum", {}),
	});
}

export type ESExprAnnEnumCase =
	| { readonly $type: "constructor", readonly name: string }
	| { readonly $type: "inline-value" }
;

export namespace ESExprAnnEnumCase {
	export const codec: ESExprCodec<ESExprAnnEnumCase> = esexpr.enumCodec({
		constructor: esexpr.caseCodec("constructor", {
			name: esexpr.positionalFieldCodec(esexpr.strCodec),
		}),
		"inline-value": esexpr.caseCodec("inline-value", {}),
	});
}

export type ESExprAnnRecordField =
	| {
		readonly $type: "keyword",
		readonly name?: string | undefined,
	}
	| { readonly $type: "dict" }
	| { readonly $type: "vararg" }
	| { readonly $type: "optional" }
	| {
		readonly $type: "default-value"
		readonly defaultValue: ESExpr,
	}
;

export namespace ESExprAnnRecordField {
	export const codec: ESExprCodec<ESExprAnnRecordField> = esexpr.enumCodec({
		keyword: esexpr.caseCodec("keyword", {
			name: esexpr.optionalPositionalFieldCodec(esexpr.undefinedOptionalCodec(esexpr.strCodec)),
		}),
		dict: esexpr.caseCodec("dict", {}),
		vararg: esexpr.caseCodec("vararg", {}),
		optional: esexpr.caseCodec("optional", {}),
		"default-value": esexpr.caseCodec("default-value", {
			defaultValue: esexpr.positionalFieldCodec(ESExpr.codec),
		})
	});
}

export type ESExprAnnExternType =
	| { readonly $type: "derive-codec" }
	| { readonly $type: "allow-optional", readonly elementType: TypeExpr }
	| { readonly $type: "allow-vararg", readonly elementType: TypeExpr }
	| { readonly $type: "allow-dict", readonly elementType: TypeExpr }
	| {
		readonly $type: "literals"
		readonly literals: ESExprAnnExternTypeLiterals,
	}
;

export namespace ESExprAnnExternType {
	export const codec: ESExprCodec<ESExprAnnExternType> = esexpr.lazyCodec(() => esexpr.enumCodec({
		"derive-codec": esexpr.caseCodec("derive-codec", {
			elementType: esexpr.positionalFieldCodec(TypeExpr.codec),
		}),
		"allow-optional": esexpr.caseCodec("allow-optional", {
			elementType: esexpr.positionalFieldCodec(TypeExpr.codec),
		}),
		"allow-vararg": esexpr.caseCodec("allow-vararg", {
			elementType: esexpr.positionalFieldCodec(TypeExpr.codec),
		}),
		"allow-dict": esexpr.caseCodec("allow-dict", {
			elementType: esexpr.positionalFieldCodec(TypeExpr.codec),
		}),
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

