import * as $esexpr from "@argon-lang/esexpr";
import * as nobleidl__core from "@argon-lang/noble-idl-core";
export interface Annotation {
    scope: nobleidl__core.String;
    value: nobleidl__core.Esexpr;
}
export namespace Annotation {
    export const codec: $esexpr.ESExprCodec<Annotation> = $esexpr.lazyCodec(() => $esexpr.recordCodec<Annotation>("annotation", {
        "scope": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
        "value": $esexpr.positionalFieldCodec(nobleidl__core.Esexpr.codec)
    }));
}
export type Definition = {
    $type: "record";
    r: RecordDefinition;
} | {
    $type: "enum";
    e: EnumDefinition;
} | {
    $type: "extern-type";
    et: ExternTypeDefinition;
} | {
    $type: "interface";
    iface: InterfaceDefinition;
};
export namespace Definition {
    export const codec: $esexpr.ESExprCodec<Definition> = $esexpr.lazyCodec(() => $esexpr.enumCodec<Definition>({
        "record": $esexpr.inlineCaseCodec("r", RecordDefinition.codec),
        "enum": $esexpr.inlineCaseCodec("e", EnumDefinition.codec),
        "extern-type": $esexpr.inlineCaseCodec("et", ExternTypeDefinition.codec),
        "interface": $esexpr.inlineCaseCodec("iface", InterfaceDefinition.codec)
    }));
}
export interface DefinitionInfo {
    name: QualifiedName;
    typeParameters: nobleidl__core.List<TypeParameter>;
    definition: Definition;
    annotations: nobleidl__core.List<Annotation>;
    isLibrary: nobleidl__core.Bool;
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
    name: nobleidl__core.String;
    fields: nobleidl__core.List<RecordField>;
    esexprOptions: nobleidl__core.OptionalField<EsexprEnumCaseOptions>;
    annotations: nobleidl__core.List<Annotation>;
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
    cases: nobleidl__core.List<EnumCase>;
    esexprOptions: nobleidl__core.OptionalField<EsexprEnumOptions>;
}
export namespace EnumDefinition {
    export const codec: $esexpr.ESExprCodec<EnumDefinition> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EnumDefinition>("enum-definition", {
        "cases": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<EnumCase>(EnumCase.codec)),
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprEnumOptions>(EsexprEnumOptions.codec))
    }));
}
export type EsexprDecodedValue = {
    $type: "record";
    t: TypeExpr;
    fields: nobleidl__core.Dict<EsexprDecodedValue>;
} | {
    $type: "enum";
    t: TypeExpr;
    caseName: nobleidl__core.String;
    fields: nobleidl__core.Dict<EsexprDecodedValue>;
} | {
    $type: "optional";
    t: TypeExpr;
    elementType: TypeExpr;
    value: nobleidl__core.OptionalField<EsexprDecodedValue>;
} | {
    $type: "vararg";
    t: TypeExpr;
    elementType: TypeExpr;
    values: nobleidl__core.List<EsexprDecodedValue>;
} | {
    $type: "dict";
    t: TypeExpr;
    elementType: TypeExpr;
    values: nobleidl__core.Dict<EsexprDecodedValue>;
} | {
    $type: "build-from";
    t: TypeExpr;
    fromType: TypeExpr;
    fromValue: EsexprDecodedValue;
} | {
    $type: "from-bool";
    t: TypeExpr;
    b: nobleidl__core.Bool;
} | {
    $type: "from-int";
    t: TypeExpr;
    i: nobleidl__core.Int;
    minInt: nobleidl__core.OptionalField<nobleidl__core.Int>;
    maxInt: nobleidl__core.OptionalField<nobleidl__core.Int>;
} | {
    $type: "from-str";
    t: TypeExpr;
    s: nobleidl__core.String;
} | {
    $type: "from-binary";
    t: TypeExpr;
    b: nobleidl__core.Binary;
} | {
    $type: "from-float32";
    t: TypeExpr;
    f: nobleidl__core.F32;
} | {
    $type: "from-float64";
    t: TypeExpr;
    f: nobleidl__core.F64;
} | {
    $type: "from-null";
    t: TypeExpr;
};
export namespace EsexprDecodedValue {
    export const codec: $esexpr.ESExprCodec<EsexprDecodedValue> = $esexpr.lazyCodec(() => $esexpr.enumCodec<EsexprDecodedValue>({
        "record": $esexpr.caseCodec("record", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "fields": $esexpr.dictFieldCodec(nobleidl__core.Dict.dictCodec<EsexprDecodedValue>(EsexprDecodedValue.codec))
        }),
        "enum": $esexpr.caseCodec("enum", {
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec),
            "caseName": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
            "fields": $esexpr.dictFieldCodec(nobleidl__core.Dict.dictCodec<EsexprDecodedValue>(EsexprDecodedValue.codec))
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
            "t": $esexpr.positionalFieldCodec(TypeExpr.codec)
        })
    }));
}
export interface EsexprEnumCaseOptions {
    caseType: EsexprEnumCaseType;
}
export namespace EsexprEnumCaseOptions {
    export const codec: $esexpr.ESExprCodec<EsexprEnumCaseOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprEnumCaseOptions>("enum-case-options", {
        "caseType": $esexpr.positionalFieldCodec(EsexprEnumCaseType.codec)
    }));
}
export type EsexprEnumCaseType = {
    $type: "constructor";
    name: nobleidl__core.String;
} | {
    $type: "inline-value";
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
    simpleEnum: nobleidl__core.Bool;
}
export namespace EsexprEnumOptions {
    export const codec: $esexpr.ESExprCodec<EsexprEnumOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprEnumOptions>("enum-options", {
        "simpleEnum": $esexpr.defaultKeywordFieldCodec("simple-enum", () => nobleidl__core.Bool.fromBoolean(false), nobleidl__core.Bool.codec)
    }));
}
export interface EsexprExternTypeLiterals {
    allowBool: nobleidl__core.Bool;
    allowInt: nobleidl__core.Bool;
    minInt: nobleidl__core.OptionalField<nobleidl__core.Int>;
    maxInt: nobleidl__core.OptionalField<nobleidl__core.Int>;
    allowStr: nobleidl__core.Bool;
    allowBinary: nobleidl__core.Bool;
    allowFloat32: nobleidl__core.Bool;
    allowFloat64: nobleidl__core.Bool;
    allowNull: nobleidl__core.Bool;
    buildLiteralFrom: nobleidl__core.OptionalField<TypeExpr>;
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
        "buildLiteralFrom": $esexpr.optionalKeywordFieldCodec("build-literal-from", nobleidl__core.OptionalField.optionalCodec<TypeExpr>(TypeExpr.codec))
    }));
}
export interface EsexprExternTypeOptions {
    allowValue: nobleidl__core.Bool;
    allowOptional: nobleidl__core.OptionalField<TypeExpr>;
    allowVararg: nobleidl__core.OptionalField<TypeExpr>;
    allowDict: nobleidl__core.OptionalField<TypeExpr>;
    literals: EsexprExternTypeLiterals;
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
    $type: "positional";
    mode: EsexprRecordPositionalMode;
} | {
    $type: "keyword";
    name: nobleidl__core.String;
    mode: EsexprRecordKeywordMode;
} | {
    $type: "dict";
    elementType: TypeExpr;
} | {
    $type: "vararg";
    elementType: TypeExpr;
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
    kind: EsexprRecordFieldKind;
}
export namespace EsexprRecordFieldOptions {
    export const codec: $esexpr.ESExprCodec<EsexprRecordFieldOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprRecordFieldOptions>("field-options", {
        "kind": $esexpr.positionalFieldCodec(EsexprRecordFieldKind.codec)
    }));
}
export type EsexprRecordKeywordMode = {
    $type: "required";
} | {
    $type: "optional";
    elementType: TypeExpr;
} | {
    $type: "default-value";
    value: EsexprDecodedValue;
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
    constructor: nobleidl__core.String;
}
export namespace EsexprRecordOptions {
    export const codec: $esexpr.ESExprCodec<EsexprRecordOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<EsexprRecordOptions>("record-options", {
        "constructor": $esexpr.keywordFieldCodec("constructor", nobleidl__core.String.codec)
    }));
}
export type EsexprRecordPositionalMode = {
    $type: "required";
} | {
    $type: "optional";
    elementType: TypeExpr;
};
export namespace EsexprRecordPositionalMode {
    export const codec: $esexpr.ESExprCodec<EsexprRecordPositionalMode> = $esexpr.lazyCodec(() => $esexpr.enumCodec<EsexprRecordPositionalMode>({
        "required": $esexpr.caseCodec("required", {}),
        "optional": $esexpr.caseCodec("optional", {
            "elementType": $esexpr.positionalFieldCodec(TypeExpr.codec)
        })
    }));
}
export interface ExternTypeDefinition {
    esexprOptions: nobleidl__core.OptionalField<EsexprExternTypeOptions>;
}
export namespace ExternTypeDefinition {
    export const codec: $esexpr.ESExprCodec<ExternTypeDefinition> = $esexpr.lazyCodec(() => $esexpr.recordCodec<ExternTypeDefinition>("extern-type-definition", {
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprExternTypeOptions>(EsexprExternTypeOptions.codec))
    }));
}
export interface InterfaceDefinition {
    methods: nobleidl__core.List<InterfaceMethod>;
}
export namespace InterfaceDefinition {
    export const codec: $esexpr.ESExprCodec<InterfaceDefinition> = $esexpr.lazyCodec(() => $esexpr.recordCodec<InterfaceDefinition>("interface-definition", {
        "methods": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<InterfaceMethod>(InterfaceMethod.codec))
    }));
}
export interface InterfaceMethod {
    name: nobleidl__core.String;
    typeParameters: nobleidl__core.List<TypeParameter>;
    parameters: nobleidl__core.List<InterfaceMethodParameter>;
    returnType: TypeExpr;
    annotations: nobleidl__core.List<Annotation>;
}
export namespace InterfaceMethod {
    export const codec: $esexpr.ESExprCodec<InterfaceMethod> = $esexpr.lazyCodec(() => $esexpr.recordCodec<InterfaceMethod>("interface-method", {
        "name": $esexpr.keywordFieldCodec("name", nobleidl__core.String.codec),
        "typeParameters": $esexpr.keywordFieldCodec("type-parameters", nobleidl__core.List.codec<TypeParameter>(TypeParameter.codec)),
        "parameters": $esexpr.keywordFieldCodec("parameters", nobleidl__core.List.codec<InterfaceMethodParameter>(InterfaceMethodParameter.codec)),
        "returnType": $esexpr.keywordFieldCodec("return-type", TypeExpr.codec),
        "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec))
    }));
}
export interface InterfaceMethodParameter {
    name: nobleidl__core.String;
    parameterType: TypeExpr;
    annotations: nobleidl__core.List<Annotation>;
}
export namespace InterfaceMethodParameter {
    export const codec: $esexpr.ESExprCodec<InterfaceMethodParameter> = $esexpr.lazyCodec(() => $esexpr.recordCodec<InterfaceMethodParameter>("interface-method-parameter", {
        "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
        "parameterType": $esexpr.positionalFieldCodec(TypeExpr.codec),
        "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec))
    }));
}
export interface NobleIdlCompileModelOptions {
    libraryFiles: nobleidl__core.List<nobleidl__core.String>;
    files: nobleidl__core.List<nobleidl__core.String>;
}
export namespace NobleIdlCompileModelOptions {
    export const codec: $esexpr.ESExprCodec<NobleIdlCompileModelOptions> = $esexpr.lazyCodec(() => $esexpr.recordCodec<NobleIdlCompileModelOptions>("options", {
        "libraryFiles": $esexpr.keywordFieldCodec("library-files", nobleidl__core.List.codec<nobleidl__core.String>(nobleidl__core.String.codec)),
        "files": $esexpr.keywordFieldCodec("files", nobleidl__core.List.codec<nobleidl__core.String>(nobleidl__core.String.codec))
    }));
}
export type NobleIdlCompileModelResult = {
    $type: "success";
    model: NobleIdlModel;
} | {
    $type: "failure";
    errors: nobleidl__core.List<nobleidl__core.String>;
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
    languageOptions: L;
    model: NobleIdlModel;
}
export namespace NobleIdlGenerationRequest {
    export function codec<L>(lCodec: $esexpr.ESExprCodec<L>): $esexpr.ESExprCodec<NobleIdlGenerationRequest<L>> { return $esexpr.lazyCodec(() => $esexpr.recordCodec<NobleIdlGenerationRequest<L>>("noble-idl-generation-request", {
        "languageOptions": $esexpr.keywordFieldCodec("language-options", lCodec),
        "model": $esexpr.keywordFieldCodec("model", NobleIdlModel.codec)
    })); }
}
export interface NobleIdlGenerationResult {
    generatedFiles: nobleidl__core.List<nobleidl__core.String>;
}
export namespace NobleIdlGenerationResult {
    export const codec: $esexpr.ESExprCodec<NobleIdlGenerationResult> = $esexpr.lazyCodec(() => $esexpr.recordCodec<NobleIdlGenerationResult>("noble-idl-generation-result", {
        "generatedFiles": $esexpr.keywordFieldCodec("generated-files", nobleidl__core.List.codec<nobleidl__core.String>(nobleidl__core.String.codec))
    }));
}
export interface NobleIdlModel {
    definitions: nobleidl__core.List<DefinitionInfo>;
}
export namespace NobleIdlModel {
    export const codec: $esexpr.ESExprCodec<NobleIdlModel> = $esexpr.lazyCodec(() => $esexpr.recordCodec<NobleIdlModel>("noble-idl-model", {
        "definitions": $esexpr.keywordFieldCodec("definitions", nobleidl__core.List.codec<DefinitionInfo>(DefinitionInfo.codec))
    }));
}
export interface PackageName {
    parts: nobleidl__core.List<nobleidl__core.String>;
}
export namespace PackageName {
    export const codec: $esexpr.ESExprCodec<PackageName> = $esexpr.lazyCodec(() => $esexpr.recordCodec<PackageName>("package-name", {
        "parts": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<nobleidl__core.String>(nobleidl__core.String.codec))
    }));
}
export interface QualifiedName {
    package: PackageName;
    name: nobleidl__core.String;
}
export namespace QualifiedName {
    export const codec: $esexpr.ESExprCodec<QualifiedName> = $esexpr.lazyCodec(() => $esexpr.recordCodec<QualifiedName>("qualified-name", {
        "package": $esexpr.positionalFieldCodec(PackageName.codec),
        "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec)
    }));
}
export interface RecordDefinition {
    fields: nobleidl__core.List<RecordField>;
    esexprOptions: nobleidl__core.OptionalField<EsexprRecordOptions>;
}
export namespace RecordDefinition {
    export const codec: $esexpr.ESExprCodec<RecordDefinition> = $esexpr.lazyCodec(() => $esexpr.recordCodec<RecordDefinition>("record-definition", {
        "fields": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<RecordField>(RecordField.codec)),
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprRecordOptions>(EsexprRecordOptions.codec))
    }));
}
export interface RecordField {
    name: nobleidl__core.String;
    fieldType: TypeExpr;
    annotations: nobleidl__core.List<Annotation>;
    esexprOptions: nobleidl__core.OptionalField<EsexprRecordFieldOptions>;
}
export namespace RecordField {
    export const codec: $esexpr.ESExprCodec<RecordField> = $esexpr.lazyCodec(() => $esexpr.recordCodec<RecordField>("record-field", {
        "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
        "fieldType": $esexpr.positionalFieldCodec(TypeExpr.codec),
        "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec)),
        "esexprOptions": $esexpr.optionalKeywordFieldCodec("esexpr-options", nobleidl__core.OptionalField.optionalCodec<EsexprRecordFieldOptions>(EsexprRecordFieldOptions.codec))
    }));
}
export type TypeExpr = {
    $type: "defined-type";
    name: QualifiedName;
    args: nobleidl__core.List<TypeExpr>;
} | {
    $type: "type-parameter";
    name: nobleidl__core.String;
};
export namespace TypeExpr {
    export const codec: $esexpr.ESExprCodec<TypeExpr> = $esexpr.lazyCodec(() => $esexpr.enumCodec<TypeExpr>({
        "defined-type": $esexpr.caseCodec("defined-type", {
            "name": $esexpr.positionalFieldCodec(QualifiedName.codec),
            "args": $esexpr.varargFieldCodec(nobleidl__core.List.varargCodec<TypeExpr>(TypeExpr.codec))
        }),
        "type-parameter": $esexpr.caseCodec("type-parameter", {
            "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec)
        })
    }));
}
export type TypeParameter = {
    $type: "type";
    name: nobleidl__core.String;
    annotations: nobleidl__core.List<Annotation>;
};
export namespace TypeParameter {
    export const codec: $esexpr.ESExprCodec<TypeParameter> = $esexpr.lazyCodec(() => $esexpr.enumCodec<TypeParameter>({
        "type": $esexpr.caseCodec("type", {
            "name": $esexpr.positionalFieldCodec(nobleidl__core.String.codec),
            "annotations": $esexpr.keywordFieldCodec("annotations", nobleidl__core.List.codec<Annotation>(Annotation.codec))
        })
    }));
}
