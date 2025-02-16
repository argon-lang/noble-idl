import * as $esexpr from "@argon-lang/esexpr";
import * as nobleidl__core from "@argon-lang/noble-idl-core";
export interface Annotation {
    readonly scope: nobleidl__core.String;
    readonly value: nobleidl__core.Esexpr;
}
export namespace Annotation {
    export const codec: $esexpr.ESExprCodec<Annotation> = $esexpr.lazyCodec(() => $esexpr.recordCodec<Annotation>("annotation", {
        "scope": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
        "value": $esexpr.positionalFieldCodec(nobleidl__core.Esexpr.codec)
    }));
}
export type Definition = {
    readonly $type: "record";
    readonly r: RecordDefinition;
} | {
    readonly $type: "enum";
    readonly e: EnumDefinition;
} | {
    readonly $type: "simple-enum";
    readonly e: SimpleEnumDefinition;
} | {
    readonly $type: "extern-type";
    readonly et: ExternTypeDefinition;
} | {
    readonly $type: "interface";
    readonly iface: InterfaceDefinition;
} | {
    readonly $type: "exception-type";
    readonly ex: ExceptionTypeDefinition;
};
export namespace Definition {
    export const codec: $esexpr.ESExprCodec<Definition> = $esexpr.lazyCodec(() => $esexpr.enumCodec<Definition>({
        "record": $esexpr.inlineCaseCodec("r", RecordDefinition.codec),
        "enum": $esexpr.inlineCaseCodec("e", EnumDefinition.codec),
        "simple-enum": $esexpr.inlineCaseCodec("e", SimpleEnumDefinition.codec),
        "extern-type": $esexpr.inlineCaseCodec("et", ExternTypeDefinition.codec),
        "interface": $esexpr.inlineCaseCodec("iface", InterfaceDefinition.codec),
        "exception-type": $esexpr.inlineCaseCodec("ex", ExceptionTypeDefinition.codec)
    }));
}
export interface DefinitionInfo {
    readonly name: QualifiedName;
    readonly typeParameters: nobleidl__core.List<TypeParameter>;
    readonly definition: Definition;
    readonly annotations: nobleidl__core.List<Annotation>;
    readonly isLibrary: nobleidl__core.Bool;
}
export namespace DefinitionInfo {
    export const codec: $esexpr.ESExprCodec<DefinitionInfo> = $esexpr.lazyCodec(() => $esexpr.recordCodec<DefinitionInfo>("definition-info", {
        "name": $esexpr.keywordFieldCodec("name", QualifiedName.codec),
        "typeParameters": $esexpr.keywordFieldCodec("type-parameters", nobleidl__core.List.codec<TypeParameter>(TypeParameter.codec)),
        "definition": $esexpr.keywordFieldCodec("definition", Definition.codec),
        "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec)),
        "isLibrary": $esexpr.keywordFieldCodec("is-library", nobleidl__core.Bool.codec)
    }));
}
export interface EnumCase {
    readonly name: nobleidl__core.String;
    readonly fields: nobleidl__core.List<RecordField>;
    readonly esexprOptions: nobleidl__core.OptionalField<EsexprEnumCaseOptions>;
    readonly annotations: nobleidl__core.List<Annotation>;
}
export namespace EnumCase {
    export const codec: $esexpr.ESExprCodec<EnumCase> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EnumCase>("enum-case", {
        "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
        "fields": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<RecordField>(RecordField.codec)),
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprEnumCaseOptions>(EsexprEnumCaseOptions.codec)),
        "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec))
    }));
}
export interface EnumDefinition {
    readonly cases: nobleidl__core.List<EnumCase>;
    readonly esexprOptions: nobleidl__core.OptionalField<EsexprEnumOptions>;
}
export namespace EnumDefinition {
    export const codec: $esexpr.ESExprCodec<EnumDefinition> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EnumDefinition>("enum-definition", {
        "cases": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<EnumCase>(EnumCase.codec)),
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprEnumOptions>(EsexprEnumOptions.codec))
    }));
}
export interface EsexprDecodedFieldValue {
    readonly name: nobleidl__core.String;
    readonly value: EsexprDecodedValue;
}
export namespace EsexprDecodedFieldValue {
    export const codec: $esexpr.ESExprCodec<EsexprDecodedFieldValue> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprDecodedFieldValue>("field-value", {
        "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
        "value": $esexpr.positionalFieldCodec(EsexprDecodedValue.codec)
    }));
}
export type EsexprDecodedValue = {
    readonly $type: "record";
    readonly t: TypeExpr;
    readonly fields: nobleidl__core.List<EsexprDecodedFieldValue>;
} | {
    readonly $type: "enum";
    readonly t: TypeExpr;
    readonly caseName: nobleidl__core.String;
    readonly fields: nobleidl__core.List<EsexprDecodedFieldValue>;
} | {
    readonly $type: "simple-enum";
    readonly t: TypeExpr;
    readonly caseName: nobleidl__core.String;
} | {
    readonly $type: "optional";
    readonly t: TypeExpr;
    readonly elementType: TypeExpr;
    readonly value: nobleidl__core.OptionalField<EsexprDecodedValue>;
} | {
    readonly $type: "vararg";
    readonly t: TypeExpr;
    readonly elementType: TypeExpr;
    readonly values: nobleidl__core.List<EsexprDecodedValue>;
} | {
    readonly $type: "dict";
    readonly t: TypeExpr;
    readonly elementType: TypeExpr;
    readonly values: nobleidl__core.Dict<EsexprDecodedValue>;
} | {
    readonly $type: "build-from";
    readonly t: TypeExpr;
    readonly fromType: TypeExpr;
    readonly fromValue: EsexprDecodedValue;
} | {
    readonly $type: "from-bool";
    readonly t: TypeExpr;
    readonly b: nobleidl__core.Bool;
} | {
    readonly $type: "from-int";
    readonly t: TypeExpr;
    readonly i: nobleidl__core.Int;
    readonly minInt: nobleidl__core.OptionalField<nobleidl__core.Int>;
    readonly maxInt: nobleidl__core.OptionalField<nobleidl__core.Int>;
} | {
    readonly $type: "from-str";
    readonly t: TypeExpr;
    readonly s: nobleidl__core.String;
} | {
    readonly $type: "from-binary";
    readonly t: TypeExpr;
    readonly b: nobleidl__core.Binary;
} | {
    readonly $type: "from-float32";
    readonly t: TypeExpr;
    readonly f: nobleidl__core.F32;
} | {
    readonly $type: "from-float64";
    readonly t: TypeExpr;
    readonly f: nobleidl__core.F64;
} | {
    readonly $type: "from-null";
    readonly t: TypeExpr;
    readonly level: nobleidl__core.OptionalField<nobleidl__core.Nat>;
    readonly maxLevel: nobleidl__core.OptionalField<nobleidl__core.Nat>;
};
export namespace EsexprDecodedValue {
    export const codec: $esexpr.ESExprCodec<EsexprDecodedValue> = $esexpr.lazyCodec(() => $esexpr.enumCodec<EsexprDecodedValue>({
        "record": $esexpr.caseCodec("record", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "fields": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<EsexprDecodedFieldValue>(EsexprDecodedFieldValue.codec))
        }),
        "enum": $esexpr.caseCodec("enum", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "caseName": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
            "fields": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<EsexprDecodedFieldValue>(EsexprDecodedFieldValue.codec))
        }),
        "simple-enum": $esexpr.caseCodec("simple-enum", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "caseName": $esexpr.positionalFieldCodec(nobleidl__core.String.codec)
        }),
        "optional": $esexpr.caseCodec("optional", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "elementType": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "value": $esexpr.optionalPositionalFieldCodec(nobleidl__core.OptionalField.optionalCodec<EsexprDecodedValue>(EsexprDecodedValue.codec))
        }),
        "vararg": $esexpr.caseCodec("vararg", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "elementType": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "values": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<EsexprDecodedValue>(EsexprDecodedValue.codec))
        }),
        "dict": $esexpr.caseCodec("dict", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "elementType": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "values": $esexpr.dictFieldCodec(nobleidl__core.Dict.dictCodec<EsexprDecodedValue>(EsexprDecodedValue.codec))
        }),
        "build-from": $esexpr.caseCodec("build-from", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "fromType": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "fromValue": $esexpr.positionalFieldCodec(EsexprDecodedValue.codec)
        }),
        "from-bool": $esexpr.caseCodec("from-bool", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "b": $esexpr.positionalFieldCodec(nobleidl__core.Bool.codec)
        }),
        "from-int": $esexpr.caseCodec("from-int", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "i": $esexpr.positionalFieldCodec(nobleidl__core.Int.codec),
            "minInt": $esexpr.optionalKeywordFieldCodec("min-int", nobleidl__core.OptionalField.optionalCodec<nobleidl__core.Int>(nobleidl__core.Int.codec)),
            "maxInt": $esexpr.optionalKeywordFieldCodec("max-int", nobleidl__core.OptionalField.optionalCodec<nobleidl__core.Int>(nobleidl__core.Int.codec))
        }),
        "from-str": $esexpr.caseCodec("from-str", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "s": $esexpr.positionalFieldCodec(nobleidl__core.String.codec)
        }),
        "from-binary": $esexpr.caseCodec("from-binary", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "b": $esexpr.positionalFieldCodec(nobleidl__core.Binary.codec)
        }),
        "from-float32": $esexpr.caseCodec("from-float32", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "f": $esexpr.positionalFieldCodec(nobleidl__core.F32.codec)
        }),
        "from-float64": $esexpr.caseCodec("from-float64", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "f": $esexpr.positionalFieldCodec(nobleidl__core.F64.codec)
        }),
        "from-null": $esexpr.caseCodec("from-null", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "level": $esexpr.optionalPositionalFieldCodec(nobleidl__core.OptionalField.optionalCodec<nobleidl__core.Nat>(nobleidl__core.Nat.codec)),
            "maxLevel": $esexpr.optionalKeywordFieldCodec("max-level", nobleidl__core.OptionalField.optionalCodec<nobleidl__core.Nat>(nobleidl__core.Nat.codec))
        })
    }));
}
export interface EsexprEnumCaseOptions {
    readonly caseType: EsexprEnumCaseType;
}
export namespace EsexprEnumCaseOptions {
    export const codec: $esexpr.ESExprCodec<EsexprEnumCaseOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprEnumCaseOptions>("enum-case-options", {
        "caseType": $esexpr.positionalFieldCodec(EsexprEnumCaseType.codec)
    }));
}
export type EsexprEnumCaseType = {
    readonly $type: "constructor";
    readonly name: nobleidl__core.String;
} | {
    readonly $type: "inline-value";
};
export namespace EsexprEnumCaseType {
    export const codec: $esexpr.ESExprCodec<EsexprEnumCaseType> = $esexpr.lazyCodec(() => $esexpr.enumCodec<EsexprEnumCaseType>({
        "constructor": $esexpr.caseCodec("constructor", {
            "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec)
        }),
        "inline-value": $esexpr.caseCodec("inline-value", {})
    }));
}
export interface EsexprEnumOptions {
}
export namespace EsexprEnumOptions {
    export const codec: $esexpr.ESExprCodec<EsexprEnumOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprEnumOptions>("enum-options", {}));
}
export interface EsexprExternTypeLiterals {
    readonly allowBool: nobleidl__core.Bool;
    readonly allowInt: nobleidl__core.Bool;
    readonly minInt: nobleidl__core.OptionalField<nobleidl__core.Int>;
    readonly maxInt: nobleidl__core.OptionalField<nobleidl__core.Int>;
    readonly allowStr: nobleidl__core.Bool;
    readonly allowBinary: nobleidl__core.Bool;
    readonly allowFloat32: nobleidl__core.Bool;
    readonly allowFloat64: nobleidl__core.Bool;
    readonly allowNull: nobleidl__core.Bool;
    readonly nullMaxLevel: nobleidl__core.OptionalField<nobleidl__core.Nat>;
    readonly buildLiteralFrom: nobleidl__core.OptionalField<TypeExpr>;
    readonly buildLiteralFromAdjustNull: nobleidl__core.Bool;
}
export namespace EsexprExternTypeLiterals {
    export const codec: $esexpr.ESExprCodec<EsexprExternTypeLiterals> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprExternTypeLiterals>("literals", {
        "allowBool": $esexpr.defaultKeywordFieldCodec("allow-bool", () => nobleidl__core.Bool.fromBoolean(false), nobleidl__core.Bool.codec),
        "allowInt": $esexpr.defaultKeywordFieldCodec("allow-int", () => nobleidl__core.Bool.fromBoolean(false), nobleidl__core.Bool.codec),
        "minInt": $esexpr.optionalKeywordFieldCodec("min-int", nobleidl__core.OptionalField.optionalCodec<nobleidl__core.Int>(nobleidl__core.Int.codec)),
        "maxInt": $esexpr.optionalKeywordFieldCodec("max-int", nobleidl__core.OptionalField.optionalCodec<nobleidl__core.Int>(nobleidl__core.Int.codec)),
        "allowStr": $esexpr.defaultKeywordFieldCodec("allow-str", () => nobleidl__core.Bool.fromBoolean(false), nobleidl__core.Bool.codec),
        "allowBinary": $esexpr.defaultKeywordFieldCodec("allow-binary", () => nobleidl__core.Bool.fromBoolean(false), nobleidl__core.Bool.codec),
        "allowFloat32": $esexpr.defaultKeywordFieldCodec("allow-float32", () => nobleidl__core.Bool.fromBoolean(false), nobleidl__core.Bool.codec),
        "allowFloat64": $esexpr.defaultKeywordFieldCodec("allow-float64", () => nobleidl__core.Bool.fromBoolean(false), nobleidl__core.Bool.codec),
        "allowNull": $esexpr.defaultKeywordFieldCodec("allow-null", () => nobleidl__core.Bool.fromBoolean(false), nobleidl__core.Bool.codec),
        "nullMaxLevel": $esexpr.optionalKeywordFieldCodec("null-max-level", nobleidl__core.OptionalField.optionalCodec<nobleidl__core.Nat>(nobleidl__core.Nat.codec)),
        "buildLiteralFrom": $esexpr.optionalKeywordFieldCodec("build-literal-from", nobleidl__core.OptionalField.optionalCodec<TypeExpr>(TypeExpr.codec)),
        "buildLiteralFromAdjustNull": $esexpr.defaultKeywordFieldCodec("build-literal-from-adjust-null", () => nobleidl__core.Bool.fromBoolean(false), nobleidl__core.Bool.codec)
    }));
}
export interface EsexprExternTypeOptions {
    readonly allowValue: nobleidl__core.Bool;
    readonly allowOptional: nobleidl__core.OptionalField<TypeExpr>;
    readonly allowVararg: nobleidl__core.OptionalField<TypeExpr>;
    readonly allowDict: nobleidl__core.OptionalField<TypeExpr>;
    readonly literals: EsexprExternTypeLiterals;
}
export namespace EsexprExternTypeOptions {
    export const codec: $esexpr.ESExprCodec<EsexprExternTypeOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprExternTypeOptions>("extern-type-options", {
        "allowValue": $esexpr.defaultKeywordFieldCodec("allow-value", () => nobleidl__core.Bool.fromBoolean(false), nobleidl__core.Bool.codec),
        "allowOptional": $esexpr.optionalKeywordFieldCodec("allow-optional", nobleidl__core.OptionalField.optionalCodec<TypeExpr>(TypeExpr.codec)),
        "allowVararg": $esexpr.optionalKeywordFieldCodec("allow-vararg", nobleidl__core.OptionalField.optionalCodec<TypeExpr>(TypeExpr.codec)),
        "allowDict": $esexpr.optionalKeywordFieldCodec("allow-dict", nobleidl__core.OptionalField.optionalCodec<TypeExpr>(TypeExpr.codec)),
        "literals": $esexpr.keywordFieldCodec("literals", EsexprExternTypeLiterals.codec)
    }));
}
export type EsexprRecordFieldKind = {
    readonly $type: "positional";
    readonly mode: EsexprRecordPositionalMode;
} | {
    readonly $type: "keyword";
    readonly name: nobleidl__core.String;
    readonly mode: EsexprRecordKeywordMode;
} | {
    readonly $type: "dict";
    readonly elementType: TypeExpr;
} | {
    readonly $type: "vararg";
    readonly elementType: TypeExpr;
};
export namespace EsexprRecordFieldKind {
    export const codec: $esexpr.ESExprCodec<EsexprRecordFieldKind> = $esexpr.lazyCodec(() => $esexpr.enumCodec<EsexprRecordFieldKind>({
        "positional": $esexpr.caseCodec("positional", {
            "mode": $esexpr.positionalFieldCodec(EsexprRecordPositionalMode.codec)
        }),
        "keyword": $esexpr.caseCodec("keyword", {
            "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
            "mode": $esexpr.positionalFieldCodec(EsexprRecordKeywordMode.codec)
        }),
        "dict": $esexpr.caseCodec("dict", {
            "elementType": $esexpr.positionalFieldCodec(TypeExpr.codec)
        }),
        "vararg": $esexpr.caseCodec("vararg", {
            "elementType": $esexpr.positionalFieldCodec(TypeExpr.codec)
        })
    }));
}
export interface EsexprRecordFieldOptions {
    readonly kind: EsexprRecordFieldKind;
}
export namespace EsexprRecordFieldOptions {
    export const codec: $esexpr.ESExprCodec<EsexprRecordFieldOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprRecordFieldOptions>("field-options", {
        "kind": $esexpr.positionalFieldCodec(EsexprRecordFieldKind.codec)
    }));
}
export type EsexprRecordKeywordMode = {
    readonly $type: "required";
} | {
    readonly $type: "optional";
    readonly elementType: TypeExpr;
} | {
    readonly $type: "default-value";
    readonly value: EsexprDecodedValue;
};
export namespace EsexprRecordKeywordMode {
    export const codec: $esexpr.ESExprCodec<EsexprRecordKeywordMode> = $esexpr.lazyCodec(() => $esexpr.enumCodec<EsexprRecordKeywordMode>({
        "required": $esexpr.caseCodec("required", {}),
        "optional": $esexpr.caseCodec("optional", {
            "elementType": $esexpr.positionalFieldCodec(TypeExpr.codec)
        }),
        "default-value": $esexpr.caseCodec("default-value", {
            "value": $esexpr.positionalFieldCodec(EsexprDecodedValue.codec)
        })
    }));
}
export interface EsexprRecordOptions {
    readonly constructor: nobleidl__core.String;
}
export namespace EsexprRecordOptions {
    export const codec: $esexpr.ESExprCodec<EsexprRecordOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprRecordOptions>("record-options", {
        "constructor": $esexpr.keywordFieldCodec("constructor", nobleidl__core.String.codec)
    }));
}
export type EsexprRecordPositionalMode = {
    readonly $type: "required";
} | {
    readonly $type: "optional";
    readonly elementType: TypeExpr;
};
export namespace EsexprRecordPositionalMode {
    export const codec: $esexpr.ESExprCodec<EsexprRecordPositionalMode> = $esexpr.lazyCodec(() => $esexpr.enumCodec<EsexprRecordPositionalMode>({
        "required": $esexpr.caseCodec("required", {}),
        "optional": $esexpr.caseCodec("optional", {
            "elementType": $esexpr.positionalFieldCodec(TypeExpr.codec)
        })
    }));
}
export interface EsexprSimpleEnumCaseOptions {
    readonly name: nobleidl__core.String;
}
export namespace EsexprSimpleEnumCaseOptions {
    export const codec: $esexpr.ESExprCodec<EsexprSimpleEnumCaseOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprSimpleEnumCaseOptions>("simple-enum-case-options", {
        "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec)
    }));
}
export interface EsexprSimpleEnumOptions {
}
export namespace EsexprSimpleEnumOptions {
    export const codec: $esexpr.ESExprCodec<EsexprSimpleEnumOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprSimpleEnumOptions>("simple-enum-options", {}));
}
export interface ExceptionTypeDefinition {
    readonly information: TypeExpr;
}
export namespace ExceptionTypeDefinition {
    export const codec: $esexpr.ESExprCodec<ExceptionTypeDefinition> = $esexpr.lazyCodec(() => $esexpr.recordCodec<ExceptionTypeDefinition>("exception-type-definition", {
        "information": $esexpr.positionalFieldCodec(TypeExpr.codec)
    }));
}
export interface ExternTypeDefinition {
    readonly esexprOptions: nobleidl__core.OptionalField<EsexprExternTypeOptions>;
}
export namespace ExternTypeDefinition {
    export const codec: $esexpr.ESExprCodec<ExternTypeDefinition> = $esexpr.lazyCodec(() => $esexpr.recordCodec<ExternTypeDefinition>("extern-type-definition", {
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprExternTypeOptions>(EsexprExternTypeOptions.codec))
    }));
}
export interface InterfaceDefinition {
    readonly methods: nobleidl__core.List<InterfaceMethod>;
}
export namespace InterfaceDefinition {
    export const codec: $esexpr.ESExprCodec<InterfaceDefinition> = $esexpr.lazyCodec(() => $esexpr.recordCodec<InterfaceDefinition>("interface-definition", {
        "methods": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<InterfaceMethod>(InterfaceMethod.codec))
    }));
}
export interface InterfaceMethod {
    readonly name: nobleidl__core.String;
    readonly typeParameters: nobleidl__core.List<TypeParameter>;
    readonly parameters: nobleidl__core.List<InterfaceMethodParameter>;
    readonly returnType: TypeExpr;
    readonly throws: nobleidl__core.OptionalField<TypeExpr>;
    readonly annotations: nobleidl__core.List<Annotation>;
}
export namespace InterfaceMethod {
    export const codec: $esexpr.ESExprCodec<InterfaceMethod> = $esexpr.lazyCodec(() => $esexpr.recordCodec<InterfaceMethod>("interface-method", {
        "name": $esexpr.keywordFieldCodec("name", nobleidl__core.String.codec),
        "typeParameters": $esexpr.keywordFieldCodec("type-parameters", nobleidl__core.List.codec<TypeParameter>(TypeParameter.codec)),
        "parameters": $esexpr.keywordFieldCodec("parameters", nobleidl__core.List.codec<InterfaceMethodParameter>(InterfaceMethodParameter.codec)),
        "returnType": $esexpr.keywordFieldCodec("return-type", TypeExpr.codec),
        "throws": $esexpr.optionalKeywordFieldCodec("throws", nobleidl__core.OptionalField.optionalCodec<TypeExpr>(TypeExpr.codec)),
        "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec))
    }));
}
export interface InterfaceMethodParameter {
    readonly name: nobleidl__core.String;
    readonly parameterType: TypeExpr;
    readonly annotations: nobleidl__core.List<Annotation>;
}
export namespace InterfaceMethodParameter {
    export const codec: $esexpr.ESExprCodec<InterfaceMethodParameter> = $esexpr.lazyCodec(() => $esexpr.recordCodec<InterfaceMethodParameter>("interface-method-parameter", {
        "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
        "parameterType": $esexpr.positionalFieldCodec(TypeExpr.codec),
        "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec))
    }));
}
export interface NobleIdlCompileModelOptions {
    readonly libraryFiles: nobleidl__core.List<nobleidl__core.String>;
    readonly files: nobleidl__core.List<nobleidl__core.String>;
}
export namespace NobleIdlCompileModelOptions {
    export const codec: $esexpr.ESExprCodec<NobleIdlCompileModelOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<NobleIdlCompileModelOptions>("options", {
        "libraryFiles": $esexpr.keywordFieldCodec("library-files", nobleidl__core.List.codec<nobleidl__core.String>(nobleidl__core.String.codec)),
        "files": $esexpr.keywordFieldCodec("files", nobleidl__core.List.codec<nobleidl__core.String>(nobleidl__core.String.codec))
    }));
}
export type NobleIdlCompileModelResult = {
    readonly $type: "success";
    readonly model: NobleIdlModel;
} | {
    readonly $type: "failure";
    readonly errors: nobleidl__core.List<nobleidl__core.String>;
};
export namespace NobleIdlCompileModelResult {
    export const codec: $esexpr.ESExprCodec<NobleIdlCompileModelResult> = $esexpr.lazyCodec(() => $esexpr.enumCodec<NobleIdlCompileModelResult>({
        "success": $esexpr.caseCodec("success", {
            "model": $esexpr.positionalFieldCodec(NobleIdlModel.codec)
        }),
        "failure": $esexpr.caseCodec("failure", {
            "errors": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<nobleidl__core.String>(nobleidl__core.String.codec))
        })
    }));
}
export interface NobleIdlGenerationRequest<L> {
    readonly languageOptions: L;
    readonly model: NobleIdlModel;
}
export namespace NobleIdlGenerationRequest {
    export function codec<L>(lCodec: $esexpr.ESExprCodec<L>): $esexpr.ESExprCodec<NobleIdlGenerationRequest<L>> { return $esexpr.lazyCodec(() => $esexpr.recordCodec<NobleIdlGenerationRequest<L>>("noble-idl-generation-request", {
        "languageOptions": $esexpr.keywordFieldCodec("language-options", lCodec),
        "model": $esexpr.keywordFieldCodec("model", NobleIdlModel.codec)
    })); }
}
export interface NobleIdlGenerationResult {
    readonly generatedFiles: nobleidl__core.List<nobleidl__core.String>;
}
export namespace NobleIdlGenerationResult {
    export const codec: $esexpr.ESExprCodec<NobleIdlGenerationResult> = $esexpr.lazyCodec(() => $esexpr.recordCodec<NobleIdlGenerationResult>("noble-idl-generation-result", {
        "generatedFiles": $esexpr.keywordFieldCodec("generated-files", nobleidl__core.List.codec<nobleidl__core.String>(nobleidl__core.String.codec))
    }));
}
export interface NobleIdlModel {
    readonly definitions: nobleidl__core.List<DefinitionInfo>;
}
export namespace NobleIdlModel {
    export const codec: $esexpr.ESExprCodec<NobleIdlModel> = $esexpr.lazyCodec(() => $esexpr.recordCodec<NobleIdlModel>("noble-idl-model", {
        "definitions": $esexpr.keywordFieldCodec("definitions", nobleidl__core.List.codec<DefinitionInfo>(DefinitionInfo.codec))
    }));
}
export interface PackageName {
    readonly parts: nobleidl__core.List<nobleidl__core.String>;
}
export namespace PackageName {
    export const codec: $esexpr.ESExprCodec<PackageName> = $esexpr.lazyCodec(() => $esexpr.recordCodec<PackageName>("package-name", {
        "parts": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<nobleidl__core.String>(nobleidl__core.String.codec))
    }));
}
export interface QualifiedName {
    readonly package: PackageName;
    readonly name: nobleidl__core.String;
}
export namespace QualifiedName {
    export const codec: $esexpr.ESExprCodec<QualifiedName> = $esexpr.lazyCodec(() => $esexpr.recordCodec<QualifiedName>("qualified-name", {
        "package": $esexpr.positionalFieldCodec(PackageName.codec),
        "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec)
    }));
}
export interface RecordDefinition {
    readonly fields: nobleidl__core.List<RecordField>;
    readonly esexprOptions: nobleidl__core.OptionalField<EsexprRecordOptions>;
}
export namespace RecordDefinition {
    export const codec: $esexpr.ESExprCodec<RecordDefinition> = $esexpr.lazyCodec(() => $esexpr.recordCodec<RecordDefinition>("record-definition", {
        "fields": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<RecordField>(RecordField.codec)),
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprRecordOptions>(EsexprRecordOptions.codec))
    }));
}
export interface RecordField {
    readonly name: nobleidl__core.String;
    readonly fieldType: TypeExpr;
    readonly annotations: nobleidl__core.List<Annotation>;
    readonly esexprOptions: nobleidl__core.OptionalField<EsexprRecordFieldOptions>;
}
export namespace RecordField {
    export const codec: $esexpr.ESExprCodec<RecordField> = $esexpr.lazyCodec(() => $esexpr.recordCodec<RecordField>("record-field", {
        "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
        "fieldType": $esexpr.positionalFieldCodec(TypeExpr.codec),
        "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec)),
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprRecordFieldOptions>(EsexprRecordFieldOptions.codec))
    }));
}
export interface SimpleEnumCase {
    readonly name: nobleidl__core.String;
    readonly esexprOptions: nobleidl__core.OptionalField<EsexprSimpleEnumCaseOptions>;
    readonly annotations: nobleidl__core.List<Annotation>;
}
export namespace SimpleEnumCase {
    export const codec: $esexpr.ESExprCodec<SimpleEnumCase> = $esexpr.lazyCodec(() => $esexpr.recordCodec<SimpleEnumCase>("simple-enum-case", {
        "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprSimpleEnumCaseOptions>(EsexprSimpleEnumCaseOptions.codec)),
        "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec))
    }));
}
export interface SimpleEnumDefinition {
    readonly cases: nobleidl__core.List<SimpleEnumCase>;
    readonly esexprOptions: nobleidl__core.OptionalField<EsexprSimpleEnumOptions>;
}
export namespace SimpleEnumDefinition {
    export const codec: $esexpr.ESExprCodec<SimpleEnumDefinition> = $esexpr.lazyCodec(() => $esexpr.recordCodec<SimpleEnumDefinition>("simple-enum-definition", {
        "cases": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<SimpleEnumCase>(SimpleEnumCase.codec)),
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprSimpleEnumOptions>(EsexprSimpleEnumOptions.codec))
    }));
}
export type TypeExpr = {
    readonly $type: "defined-type";
    readonly name: QualifiedName;
    readonly args: nobleidl__core.List<TypeExpr>;
} | {
    readonly $type: "type-parameter";
    readonly name: nobleidl__core.String;
    readonly owner: TypeParameterOwner;
};
export namespace TypeExpr {
    export const codec: $esexpr.ESExprCodec<TypeExpr> = $esexpr.lazyCodec(() => $esexpr.enumCodec<TypeExpr>({
        "defined-type": $esexpr.caseCodec("defined-type", {
            "name": $esexpr.positionalFieldCodec(QualifiedName.codec),
            "args": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<TypeExpr>(TypeExpr.codec))
        }),
        "type-parameter": $esexpr.caseCodec("type-parameter", {
            "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
            "owner": $esexpr.keywordFieldCodec("owner", TypeParameterOwner.codec)
        })
    }));
}
export type TypeParameter = {
    readonly $type: "type";
    readonly name: nobleidl__core.String;
    readonly constraints: nobleidl__core.List<TypeParameterTypeConstraint>;
    readonly annotations: nobleidl__core.List<Annotation>;
};
export namespace TypeParameter {
    export const codec: $esexpr.ESExprCodec<TypeParameter> = $esexpr.lazyCodec(() => $esexpr.enumCodec<TypeParameter>({
        "type": $esexpr.caseCodec("type", {
            "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
            "constraints": $esexpr.defaultKeywordFieldCodec("constraints", () => nobleidl__core.List.buildFrom<TypeParameterTypeConstraint>({
                values: nobleidl__core.List.fromArray<TypeParameterTypeConstraint>([])
            }), nobleidl__core.List.codec<TypeParameterTypeConstraint>(TypeParameterTypeConstraint.codec)),
            "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec))
        })
    }));
}
export type TypeParameterOwner = "by-type" | "by-method";
export namespace TypeParameterOwner {
    export const codec: $esexpr.ESExprCodec<TypeParameterOwner> = $esexpr.lazyCodec(() => $esexpr.simpleEnumCodec<TypeParameterOwner>({
        "by-type": "by-type",
        "by-method": "by-method"
    }));
}
export type TypeParameterTypeConstraint = {
    readonly $type: "exception";
};
export namespace TypeParameterTypeConstraint {
    export const codec: $esexpr.ESExprCodec<TypeParameterTypeConstraint> = $esexpr.lazyCodec(() => $esexpr.enumCodec<TypeParameterTypeConstraint>({
        "exception": $esexpr.caseCodec("exception", {})
    }));
}
