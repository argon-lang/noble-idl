#!/usr/bin/env node

import { compile } from "./index.js";
import { parseArgs } from "node:util";
import * as process from "node:process";
import { loadCompilerOptions } from "./package_options.js";

const args = parseArgs({
	options: {
		"package-dir": {
			type: "string",
		},

		"output-dir": {
			type: "string",
		}
	},
});

const packageDir = args.values["package-dir"] ?? process.cwd();

const outputDir = args.values["output-dir"];

if(outputDir === undefined) {
	console.error("--output-dir must be specified");
	process.exit(1);
}

const compilerOptions = await loadCompilerOptions(packageDir, outputDir);
console.log("compiler options");
console.log(compilerOptions);
await compile(compilerOptions);
