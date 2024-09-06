#!/bin/bash -e

cd "$(dirname "$0")"

pushd ../.. > /dev/null
cargo build -p noble-idl-compiler --target=wasm32-unknown-unknown --release
popd > /dev/null

mkdir -p NobleIDL.Backend/Wasm
cp ../../target/wasm32-unknown-unknown/release/noble_idl_compiler.wasm NobleIDL.Backend/Wasm/
