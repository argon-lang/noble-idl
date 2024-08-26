import type { PackageName } from "../api.js";
import type { JSLanguageOptions } from "./index.js";


export interface JSModule {
	isCurrentPackage: boolean;
	packageName: string;
	path: string;
}

export function getPackageNameStr(name: PackageName): string {
	return name.parts.join(".");
}

export function decodePackageNameStr(name: string): PackageName {
	return {
		parts: name.length === 0 ? [] : name.split("."),
	};
}

export function getPackageIdStr(name: PackageName | string): string {
	if (typeof name === "object") {
		return getPackageIdStr(getPackageNameStr(name));
	}

	return name.replace(".", "__").replace("-", "_");
}


export function getPackageMapping(options: JSLanguageOptions): Map<string, JSModule> {
	let pkgMapping = new Map<string, JSModule>();

	for (const [packageName, packageOptions] of options.packageOptions) {
		const isCurrentPackage = options.packageName == packageName;

		for (const [idlPackage, jsPath] of packageOptions.packageMapping) {
			pkgMapping.set(idlPackage, {
				isCurrentPackage,
				packageName: packageName,
				path: jsPath,
			});
		}
	}

	return pkgMapping;
}


