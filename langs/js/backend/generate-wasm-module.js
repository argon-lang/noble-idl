#!/usr/bin/env node

import * as fs from "node:fs/promises";
import * as path from "node:path";

const wasmFile = path.join(import.meta.dirname, "../../../target/wasm32-unknown-unknown/release/noble_idl_compiler.wasm");

const wasmContent = await fs.readFile(wasmFile);

await fs.mkdir(path.join(import.meta.dirname, "lib"), { recursive: true });

await fs.writeFile(path.join(import.meta.dirname, "lib/noble-idl-compiler.js"), `
const wasmModuleData = "${wasmContent.toString("base64")}";

const binaryString = atob(wasmModuleData);
const binaryArray = new Uint8Array(binaryString.length);

for (let i = 0; i < binaryString.length; i++) {
  binaryArray[i] = binaryString.charCodeAt(i);
}

const wasmModule = await WebAssembly.compile(binaryArray);
const instance = await WebAssembly.instantiate(wasmModule);

export const memory = instance.exports.memory;
export const nobleidl_alloc = instance.exports.nobleidl_alloc;
export const nobleidl_free = instance.exports.nobleidl_free;
export const nobleidl_compile_model = instance.exports.nobleidl_compile_model;


`)



