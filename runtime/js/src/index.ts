
export type String = string;
export type Binary = Uint8Array;

export type Int = bigint;
export type Nat = bigint;

export type I8 = number;
export type U8 = number;
export type I16 = number;
export type U16 = number;
export type I32 = number;
export type U32 = number;
export type I64 = bigint;
export type U64 = bigint;

export type F32 = number;
export type F64 = number;

export type Unit = undefined;

export type List<A> = readonly A[];
export type Option<A> = { readonly value: A } | null;
export type Dict<A> = ReadonlyMap<string, A>;

