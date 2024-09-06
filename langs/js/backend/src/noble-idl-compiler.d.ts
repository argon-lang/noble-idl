
export declare const memory: WebAssembly.Memory;

export declare function nobleidl_alloc(size: number): number;
export declare function nobleidl_free(ptr: number, size: number): void;

export declare function nobleidl_compile_model(options: number, options_size: number, result_size: number): number;

