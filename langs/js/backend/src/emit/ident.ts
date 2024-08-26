




export function convertIdPascal(kebab: string): string {
	return kebab
		.split('-')
		.map(segment => segment.charAt(0).toUpperCase() + segment.slice(1))
		.join('');
}

export function convertIdCamel(kebab: string): string {
	const pascalCase = convertIdPascal(kebab);
	return pascalCase.charAt(0).toLowerCase() + pascalCase.slice(1);
}

export function getUnshadowedName(name: string): string {
	return "$Unshadowed_" + convertIdPascal(name);
}



