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
