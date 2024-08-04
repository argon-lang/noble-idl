import type { ESExpr } from "@argon-lang/esexpr";
import { NobleIdlCompileModelOptions, NobleIdlCompileModelResult, type NobleIdlGenerationResult } from "./api.js";
import { emit, type JSLanguageOptions } from "./emit.js";
import * as compiler from "./noble-idl-compiler.js";

import * as esxb from "@argon-lang/esexpr/binary_format";

import * as fs from "node:fs/promises";


export interface JavaScriptIDLCompilerOptions {
    readonly languageOptions: JSLanguageOptions,

    readonly inputFiles: readonly string[],
    readonly libraryFiles: readonly string[],
}


async function loadModelOptions(options: JavaScriptIDLCompilerOptions): Promise<NobleIdlCompileModelOptions> {
    const inputFiles: string[] = [];
    const libraryFiles: string[] = [];

    for(const f of options.inputFiles) {
        inputFiles.push(await fs.readFile(f, { encoding: "utf-8" }));
    }

    for(const f of options.libraryFiles) {
        libraryFiles.push(await fs.readFile(f, { encoding: "utf-8" }));
    }

    return {
        files: inputFiles,
        libraryFiles,
    };
}


interface Buffer {
    readonly size: number,
    readonly data: number,
}

function nobleidl_alloc(size: number): Buffer {
    const [ size2, data ] = compiler.nobleidl_alloc(size);
    return { size: size2, data };
}

function nobleidl_free(b: Buffer) {
    compiler.nobleidl_free(b.size, b.data);
}

function nobleidl_compile_model(options: Buffer): Buffer {
    const [ resSize, resData ] = compiler.nobleidl_compile_model(options.size, options.data);
    return { size: resSize, data: resData };
}

async function* writeExprWithSP(expr: ESExpr): AsyncIterable<Uint8Array> {
    const spb = new esxb.StringPoolBuilder();
    const parts: Uint8Array[] = [];

    for await(const part of esxb.writeExpr(expr, spb.adapter())) {
        parts.push(part);
    }

    yield* esxb.writeExpr(esxb.StringPoolEncoded.codec.encode(spb.toStringPool().toEncoded()), new esxb.ArrayStringPool([]));
    yield* parts;
}

async function streamToBuffer(data: AsyncIterable<Uint8Array>): Promise<Buffer> {
    const parts: Uint8Array[] = [];

    for await(const part of data) {
        parts.push(part);
    }

    let size = 0;
    for(const part of parts) {
        size += part.length;
    }

    const buffer = nobleidl_alloc(size);

    const memory = new Uint8Array(compiler.memory.buffer);

    let offset = buffer.data;
    for(const part of parts) {
        memory.set(part, offset);
        offset += part.length;
    }

    return buffer;
}

function bufferToArray(b: Buffer): Uint8Array {
    return new Uint8Array(compiler.memory.buffer, b.data, b.size);
}

async function* singleton(b: Uint8Array): AsyncIterable<Uint8Array> {
    yield b;
}

async function loadModel(options: NobleIdlCompileModelOptions): Promise<NobleIdlCompileModelResult> {
    const optionsBuffer = await streamToBuffer(writeExprWithSP(NobleIdlCompileModelOptions.codec.encode(options)));
    try {
        const resultBuffer = nobleidl_compile_model(optionsBuffer);
        const resultArray = bufferToArray(resultBuffer);

        try {
            let expr: ESExpr | null = null;

            for await(const exprItem of { [Symbol.asyncIterator]() { return esxb.readExprStreamEmbeddedStringPool(singleton(resultArray)); } }) {
                if(expr !== null) {
                    throw new Error("Extra model found");
                }

                expr = exprItem;
            }

            if(expr === null) {
                throw new Error("No model found");
            }

            const result = NobleIdlCompileModelResult.codec.decode(expr);
            if(!result.success) {
                throw new Error("Invalid model: " + result.message + "\n" + JSON.stringify(result.path));
            }

            return result.value;
        }
        finally {
            nobleidl_free(resultBuffer);
        }
    }
    finally {
        nobleidl_free(optionsBuffer);
    }
}


export async function compile(options: JavaScriptIDLCompilerOptions): Promise<NobleIdlGenerationResult> {
    const model = await loadModel(await loadModelOptions(options));

    if(model.$type === "failure") {
        throw new Error("Error loading module: " + model.errors.join("\n"));
    }


    return await emit({
        languageOptions: options.languageOptions,
        model: model.model,
    });
}


