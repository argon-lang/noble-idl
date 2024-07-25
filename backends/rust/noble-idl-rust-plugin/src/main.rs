fn main() {
    noble_idl_rust_plugin::emit::emit_from_stream(std::io::stdin(), std::io::stdout())
        .unwrap()
}
