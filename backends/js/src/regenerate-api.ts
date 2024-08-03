#!/usr/bin/env node

import { compile, type JavaScriptIDLCompilerOptions } from "./index.js";
import * as path from "node:path";


const dir = import.meta.dirname;
console.log(dir);

const compilerOptions: JavaScriptIDLCompilerOptions = {
	languageOptions: {
		outputDir: path.join(dir, "../src"),
		packageName: "@argon-lang/noble-idl-compiler-js",
		packageOptions: new Map([
			[ "@argon-lang/noble-idl-runtime", {
				packageMapping: new Map([
					[ "nobleidl.core", "" ],
				]),
			} ],
			[ "@argon-lang/noble-idl-api", {
				packageMapping: new Map([
					[ "nobleidl.compiler.api", "api" ],
				]),
			} ],
		]),
	},

	inputFiles: [
		path.join(dir, "../../noble-idl/compiler-api.nidl"),
	],

	libraryFiles: [
		path.join(dir, "../../../runtime/noble-idl/nobleidl-core.nidl"),
	],
};
await compile(compilerOptions);


