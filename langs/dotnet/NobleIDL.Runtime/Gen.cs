namespace NobleIDL.Runtime
{
    [global::ESExpr.Runtime.ESExprCodec, global::ESExpr.Runtime.Constructor("list")]
    public sealed partial record ListRepr<A>
    {
        [global::ESExpr.Runtime.Vararg]
        public required global::ESExpr.Runtime.VList<A> Values { get; init; }
    }
}