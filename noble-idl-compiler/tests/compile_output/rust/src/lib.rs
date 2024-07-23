
include!(concat!(env!("OUT_DIR"), "/noble_idl/nobleidl/test.rs"));

#[derive(Debug, Clone, PartialEq)]
pub struct MyExtern;

#[derive(Debug, Clone, PartialEq)]
pub struct MyExtern2<A>(A);


