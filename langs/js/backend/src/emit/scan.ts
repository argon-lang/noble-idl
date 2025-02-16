import { type DefinitionInfo, type EnumDefinition, type InterfaceDefinition, type InterfaceMethod, PackageName, type RecordDefinition, type RecordField, type TypeExpr, type TypeParameter, ExternTypeDefinition, ExceptionTypeDefinition } from "../api.js";

import { getPackageNameStr } from "./package.js";
import { isSamePackage } from "../api-util.js";

export interface ModuleMetadata {
	referencedPackages: Map<string, ReferencedPackageInfo>;
	externTypes: Map<string, ExternTypeInfo>;
	shadowedTypes: Set<string>;
	needsEsexprImport: boolean,
	needsUtilImport: boolean,
}

interface TypeReferenceContext {
	readonly isTypeOnly: boolean;
	readonly typeParameters: ReadonlySet<string>,
}

interface ReferencedPackageInfo {
	isTypeOnly: boolean;
}

interface ExternTypeInfo {
	isReferencedInModule: boolean;
	isTypeOnly: boolean;
}

export class ModuleScanner {

	constructor(
		private currentPackage: PackageName,
		private definitions: readonly DefinitionInfo[],
	) { }

	metadata: ModuleMetadata = {
		referencedPackages: new Map(),
		externTypes: new Map(),
		shadowedTypes: new Set(),
		needsEsexprImport: false,
		needsUtilImport: false,
	};


	scanModule(): void {
		for (const def of this.definitions) {
			if (def.definition.$type !== "extern-type") {
				continue;
			}
			this.#scanExtern(def, def.definition.et);
		}
		for (const def of this.definitions) {
			this.#scanDefinition(def);
		}
	}


	#addPackage(packageName: PackageName, refContext: TypeReferenceContext): void {
		const packageNameStr = getPackageNameStr(packageName);
		const prevInfo = this.metadata.referencedPackages.get(packageNameStr);
		const combinedInfo: ReferencedPackageInfo = {
			isTypeOnly: refContext.isTypeOnly && (prevInfo?.isTypeOnly ?? true),
		};

		this.metadata.referencedPackages.set(packageNameStr, combinedInfo);
	}


	#scanDefinition(def: DefinitionInfo): void {
		switch (def.definition.$type) {
			case "record":
				this.#scanRecord(def, def.definition.r);
				break;

			case "enum":
				this.#scanEnum(def, def.definition.e);
				break;

			case "interface":
				this.#scanInterface(def, def.definition.iface);
				break;

			case "exception-type":
				this.#scanExceptionType(def, def.definition.ex);
				break;

			case "simple-enum": // Simple enums can't reference other types.
			case "extern-type": // Extern types are scanned first.
				break;
		}
	}

	#scanRecord(def: DefinitionInfo, r: RecordDefinition): void {
		const hasEsexpr = r.esexprOptions !== undefined;

		const refContext: TypeReferenceContext = {
			isTypeOnly: !hasEsexpr,
			typeParameters: this.#buildTypeParameters(def.typeParameters),
		};

		this.metadata.needsEsexprImport ||= hasEsexpr;


		for (const f of r.fields) {
			this.#scanField(f, refContext);
		}
	}

	#scanEnum(def: DefinitionInfo, e: EnumDefinition): void {
		let hasEsexpr = e.esexprOptions !== undefined;

		const refContext: TypeReferenceContext = {
			isTypeOnly: !hasEsexpr,
			typeParameters: this.#buildTypeParameters(def.typeParameters),
		};


		for (const c of e.cases) {
			for (const f of c.fields) {
				this.#scanField(f, refContext);
			}
		}
	}

	#scanExtern(def: DefinitionInfo, ext: ExternTypeDefinition): void {
		let hasEsexpr = ext.esexprOptions !== undefined;

		this.metadata.externTypes.set(def.name.name, {
			isReferencedInModule: false,
			isTypeOnly: !hasEsexpr,
		});
	}

	#scanInterface(def: DefinitionInfo, i: InterfaceDefinition): void {
		const refContext: TypeReferenceContext = {
			isTypeOnly: true,
			typeParameters: this.#buildTypeParameters(def.typeParameters),
		};

		for (const m of i.methods) {
			this.#scanMethod(m, refContext);
		}
	}

	#scanExceptionType(def: DefinitionInfo, ex: ExceptionTypeDefinition): void {
		const refContext: TypeReferenceContext = {
			isTypeOnly: true,
			typeParameters: this.#buildTypeParameters(def.typeParameters),
		};

		this.metadata.needsUtilImport = true;

		this.#scanType(ex.information, refContext);
	}

	#scanField(field: RecordField, refContext: TypeReferenceContext): void {
		this.#scanType(field.fieldType, refContext);
	}

	#scanMethod(method: InterfaceMethod, refContext: TypeReferenceContext): void {
		const refContext2: TypeReferenceContext = {
			...refContext,
			typeParameters: this.#buildTypeParameters(method.typeParameters, refContext.typeParameters),
		};

		for(const tp of method.typeParameters) {
			if(tp.constraints.some(c => c.$type === "exception")) {
				this.metadata.needsUtilImport = true;
			}
		}

		for(const p of method.parameters) {
			this.#scanType(p.parameterType, refContext2);
		}
		this.#scanType(method.returnType, refContext2);

		if(method.throws !== undefined) {
			this.metadata.needsUtilImport = true;
			this.#scanType(method.throws, refContext2);
		}
	}

	#scanType(t: TypeExpr, refContext: TypeReferenceContext): void {
		switch (t.$type) {
			case "defined-type":
				if (isSamePackage(t.name.package, this.currentPackage)) {
					const externInfo = this.metadata.externTypes.get(t.name.name);
					if (externInfo !== undefined) {
						externInfo.isReferencedInModule = true;
					}

					if (refContext.typeParameters.has(t.name.name)) {
						this.metadata.shadowedTypes.add(t.name.name);
					}
				}
				else {
					this.#addPackage(t.name.package, refContext);
				}

				for (const arg of t.args) {
					this.#scanType(arg, refContext);
				}
				break;

			case "type-parameter":
				break;
		}
	}

	#buildTypeParameters(typeParams: readonly TypeParameter[], existing?: ReadonlySet<string>): Set<string> {
		const params = new Set(existing);
		for (const tp of typeParams) {
			params.add(tp.name);
		}
		return params;
	}
}


