
// export declare function getMemory(): WebAssembly.Memory;
export declare const memory: WebAssembly.Memory;

export declare function nobleidl_alloc(size: number): [number, number];
export declare function nobleidl_free(size: number, data: number): void;

export declare function nobleidl_compile_model(options_size: number, options_data: number): [number, number];

