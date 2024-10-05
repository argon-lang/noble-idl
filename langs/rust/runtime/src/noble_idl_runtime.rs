#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "dict"]
pub struct DictRepr<A> {
    #[dict]
    pub values: crate::Dict<A>,
}
#[derive(
    ::std::fmt::Debug,
    ::std::clone::Clone,
    ::std::cmp::PartialEq,
    ::esexpr::ESExprCodec
)]
#[constructor = "list"]
pub struct ListRepr<A> {
    #[vararg]
    pub values: crate::List<A>,
}
