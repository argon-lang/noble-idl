#!/usr/bin/env node

import { compile } from "./index.js";
import * as path from "node:path";


const dir = import.meta.dirname;

await compile({
	languageOptions: {
		outputDir: path.join(dir, "../../runtime/src"),
		packageName: "@argon-lang/noble-idl-core",
		packageOptions: new Map([
			[ "@argon-lang/noble-idl-core", {
				packageMapping: new Map([
					[ "nobleidl.core", "" ],
				]),
			} ],
		]),
	},

	inputFiles: [
		path.join(dir, "../../../noble-idl/runtime/nobleidl-core.nidl"),
	],

	libraryFiles: [
	],
});

await compile({
	languageOptions: {
		outputDir: path.join(dir, "../src"),
		packageName: "@argon-lang/noble-idl-compiler-js",
		packageOptions: new Map([
			[ "@argon-lang/noble-idl-core", {
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
		path.join(dir, "../../../noble-idl/backend/compiler-api.nidl"),
	],

	libraryFiles: [
		path.join(dir, "../../../noble-idl/runtime/nobleidl-core.nidl"),
	],
});



