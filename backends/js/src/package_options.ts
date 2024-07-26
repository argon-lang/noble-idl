import * as fs from "node:fs/promises";
import * as path  from "node:path";
import type { JavaScriptIDLCompilerOptions } from "./index.js";
import type { PackageOptions } from "./emit.js";
import validateNpmModulePath from "validate-npm-package-name";



export async function loadCompilerOptions(packageDir: string, outputDir: string): Promise<JavaScriptIDLCompilerOptions> {
	console.log("loadCompilerOptions");
	const packageJsonOptions = await loadOptionsFromFile(path.join(packageDir, "package.json"));
	if(packageJsonOptions === null) {
		throw new Error("Could not load NobleIDL options from package.json");
	}

	const libraryFiles: string[] = [];
	const packageOptions = new Map<string, PackageOptions>();

	const createPackageOptions = (packageJsonOptions: NobleIDLPackageJsonOptions): PackageOptions => {
		return {
			packageMapping: packageJsonOptions.packageMapping,
		};
	};

	const loadOptionsLib = async (dir: string) => {
		const packageJsonOptions = await loadOptionsFromFile(path.join(dir, "package.json"));
		console.log(dir);
		console.log(packageJsonOptions);
		if(packageJsonOptions === null) {
			return;
		}

		for(const libFile of packageJsonOptions.inputFiles) {
			libraryFiles.push(path.join(dir, libFile));
		}

		packageOptions.set(packageJsonOptions.packageName, createPackageOptions(packageJsonOptions));
	};

	const nodeModulesDir = path.join(packageDir, "node_modules");
	if(await fs.access(nodeModulesDir).then(() => true).catch(() => false)) {
		for(const depPackageDirName of await fs.readdir(nodeModulesDir)) {
			const depPackageDir = path.join(nodeModulesDir, depPackageDirName);

			if(!(await fs.stat(depPackageDir)).isDirectory()) {
				continue;
			}

			if(depPackageDirName.startsWith("@")) {
				for(const depPackageSubDirName of await fs.readdir(depPackageDir)) {
					const depPackageSubDir = path.join(depPackageDir, depPackageSubDirName);

					if(!(await fs.stat(depPackageSubDir)).isDirectory()) {
						continue;
					}

					if(validateNpmModulePath(depPackageDirName + "/" + depPackageSubDirName)) {
						await loadOptionsLib(depPackageSubDir);
					}
				}
			}
			else if(validateNpmModulePath(depPackageDirName)) {
				await loadOptionsLib(depPackageDir);
			}
		}
	}

	packageOptions.set(packageJsonOptions.packageName, createPackageOptions(packageJsonOptions));

	const res = {
		languageOptions: {
			packageName: packageJsonOptions.packageName,
			outputDir,
			packageOptions,
		},
		inputFiles: packageJsonOptions.inputFiles.map(inputFile => path.join(packageDir, inputFile)),
		libraryFiles,
	};

	console.log("loadCompilerOptions done");
	console.log(res);

	return res;
}


interface NobleIDLPackageJsonOptions {
	readonly packageName: string;
	readonly inputFiles: readonly string[];
	readonly packageMapping: Map<string, string>;
}

function loadOptions(packageJson: unknown): NobleIDLPackageJsonOptions | null {
	if(typeof packageJson !== "object" || packageJson === null || !("NobleIDL" in packageJson)) {
		return null;
	}

	if(!("name" in packageJson)) {
		throw new Error("package name is missing");
	}

	const packageName = packageJson.name;

	if(typeof packageName !== "string") {
		throw new Error("package name must be a string");
	}


	const nobleIDLJson = packageJson.NobleIDL;

	if(typeof nobleIDLJson !== "object" || nobleIDLJson === null || !("inputFiles" in nobleIDLJson)) {
		throw new Error("NobleIDL.inputFiles is missing");
	}

	if(!(nobleIDLJson.inputFiles instanceof Array)) {
		throw new Error("NobleIDL.inputFiles must be an array");
	}

	const inputFiles: string[] = [];
	for(const inputFile of nobleIDLJson.inputFiles) {
		if(typeof inputFile !== "string") {
			throw new Error("NobleIDL.inputFiles must only contain strings");
		}

		inputFiles.push(inputFile);
	}



	if(!("packageMapping" in nobleIDLJson)) {
		throw new Error("NobleIDL.packageMapping is missing");
	}

	const packageMappingJson = nobleIDLJson.packageMapping;

	if(typeof packageMappingJson !== "object" || packageMappingJson === null) {
		throw new Error("packageMappingJson must be an object");
	}

	const packageMapping = new Map<string, string>();
	for(const [idlPackage, jsModule] of Object.entries(packageMappingJson)) {
		if(typeof jsModule !== "string") {
			throw new Error("NobleIDL.packageMapping values must only be strings");
		}

		packageMapping.set(idlPackage, jsModule);
	}

	return {
		packageName,
		inputFiles,
		packageMapping,
	};
}

async function loadOptionsFromFile(packageJsonFile: string): Promise<NobleIDLPackageJsonOptions | null> {
	let jsonStr: string;
	try {
		jsonStr = await fs.readFile(packageJsonFile, { encoding: "utf8" });
	}
	catch(e) {
		if(typeof e === "object" && e !== null && "code" in e && e.code === "ENOENT") {
			return null;
		}

		throw e;
	}

	const json = JSON.parse(jsonStr);

	return loadOptions(json);
}




