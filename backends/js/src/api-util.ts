import type { PackageName } from "./api.js";

export function isSamePackage(a: PackageName, b: PackageName): boolean {
	if(a.parts.length !== b.parts.length) {
		return false;
	}

	for(let i = 0; i < a.parts.length; ++i) {
		if(a.parts[i] !== b.parts[i]) {
			return false;
		}
	}

	return true;
}
