import * as esexpr from "@argon-lang/esexpr";
import { ESExpr, type ESExprCodec } from "@argon-lang/esexpr";


export type Esexpr = ESExpr;
export namespace Esexpr {
	export const codec: ESExprCodec<Esexpr> = ESExpr.codec;
}


export type String = string;
export namespace String {
	export const codec: ESExprCodec<string> = esexpr.strCodec;
}

export type Binary = Uint8Array;
export namespace Binary {
	export const codec: ESExprCodec<Uint8Array> = esexpr.binaryCodec;
}

export type Int = bigint;
export namespace Int {
	export const codec: ESExprCodec<bigint> = esexpr.intCodec;
}

export type Nat = bigint;
export namespace Nat {
	export const codec: ESExprCodec<bigint> = {
		get tags() {
			return esexpr.intCodec.tags;
		},

		encode(value: bigint): ESExpr {
			return value;
		},

		decode(expr: ESExpr): esexpr.DecodeResult<bigint> {
			const result = esexpr.intCodec.decode(expr);
			if(result.success && result.value < 0n) {
				return {
					success: false,
					message: "Nat values must be non-negative",
					path: { type: "current" },
				};
			}

			return result;
		}
	};
}

export type Bool = boolean;
export namespace Bool {
	export const codec: ESExprCodec<boolean> = esexpr.boolCodec;
}


export type I8 = number;
export namespace I8 {
	export const codec: ESExprCodec<number> = esexpr.signedInt8Codec;
}

export type U8 = number;
export namespace U8 {
	export const codec: ESExprCodec<number> = esexpr.unsignedInt8Codec;
}

export type I16 = number;
export namespace I16 {
	export const codec: ESExprCodec<number> = esexpr.signedInt16Codec;
}

export type U16 = number;
export namespace U16 {
	export const codec: ESExprCodec<number> = esexpr.unsignedInt16Codec;
}

export type I32 = number;
export namespace I32 {
	export const codec: ESExprCodec<number> = esexpr.signedInt32Codec;
}

export type U32 = number;
export namespace U32 {
	export const codec: ESExprCodec<number> = esexpr.unsignedInt32Codec;
}

export type I64 = bigint;
export namespace I64 {
	export const codec: ESExprCodec<bigint> = esexpr.signedInt64Codec;
}

export type U64 = bigint;
export namespace U64 {
	export const codec: ESExprCodec<bigint> = esexpr.unsignedInt64Codec;
}


export type F32 = number;
export namespace F32 {
	export const codec: ESExprCodec<number> = esexpr.float32Codec;
}

export type F64 = number;
export namespace F64 {
	export const codec: ESExprCodec<number> = esexpr.float64Codec;
}


export type Unit = undefined;

export type List<A> = readonly A[];
export namespace List {
	export function codec<A>(aCodec: ESExprCodec<A>): ESExprCodec<readonly A[]> {
		return esexpr.listCodec(aCodec);
	}

	export function varargCodec<A>(aCodec: ESExprCodec<A>): esexpr.RepeatedValuesCodec<List<A>> {
		return esexpr.arrayRepeatedValuesCodec(aCodec);
	}
}

export type Option<A> = { readonly value: A } | null;
export namespace Option {
	export function codec<A>(aCodec: ESExprCodec<A>): ESExprCodec<Option<A>> {
		return esexpr.optionCodec(aCodec);
	}
}

export type OptionalField<A> = A | undefined;
export namespace OptionalField {
	export function optionalCodec<A>(aCodec: ESExprCodec<A>): esexpr.OptionalValueCodec<OptionalField<A>> {
		return esexpr.undefinedOptionalCodec(aCodec);
	}

	export 
}

export type Dict<A> = ReadonlyMap<string, A>;
export namespace Dict {
	export function dictCodec<A>(aCodec: ESExprCodec<A>): esexpr.MappedValueCodec<Dict<A>> {
		return esexpr.mapMappedValueCodec(aCodec);
	}
}

