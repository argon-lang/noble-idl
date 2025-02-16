import * as $esexpr from "@argon-lang/esexpr";
import { Dict, List } from "./index.extern.js";
export { Binary, Bool, Dict, Esexpr, F32, F64, I16, I32, I64, I8, Int, List, Nat, Option, OptionalField, String, U16, U32, U64, U8, type Unit } from "./index.extern.js";
export interface DictRepr<A> {
    readonly values: Dict<A>;
}
export namespace DictRepr {
    export function codec<A>(aCodec: $esexpr.ESExprCodec<A>): $esexpr.ESExprCodec<DictRepr<A>> { return $esexpr.lazyCodec(() => $esexpr.recordCodec<DictRepr<A>>("dict", {
        "values": $esexpr.dictFieldCodec(Dict.dictCodec<A>(aCodec))
    })); }
}
export interface ListRepr<A> {
    readonly values: List<A>;
}
export namespace ListRepr {
    export function codec<A>(aCodec: $esexpr.ESExprCodec<A>): $esexpr.ESExprCodec<ListRepr<A>> { return $esexpr.lazyCodec(() => $esexpr.recordCodec<ListRepr<A>>("list", {
        "values": $esexpr.varargFieldCodec(List.varargCodec<A>(aCodec))
    })); }
}
