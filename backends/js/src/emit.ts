import { ESExprAnnExternType, type DefinitionInfo, type EnumCase, type EnumDefinition, type InterfaceDefinition, type InterfaceMethod, type NobleIDLGenerationRequest, type NobleIDLGenerationResult, type NobleIDLModel, PackageName, type RecordDefinition, type RecordField, type TypeExpr, type TypeParameter, ESExprAnnRecord, Annotation, ESExprAnnEnum, ESExprAnnRecordField, ESExprAnnEnumCase } from "./api.js";

import * as path from "node:path";
import * as posixPath from "node:path/posix";
import * as fs from "node:fs/promises";
import * as os from "node:os";
import * as ts from "typescript";
import type { ESExprCodec } from "@argon-lang/esexpr";

export interface JSLanguageOptions {
    // The name of the NPM package (in package.json)
    packageName: string,

    // The directory where files will be written.
    outputDir: string,

    packageOptions: ReadonlyMap<string, PackageOptions>,
}

export interface PackageOptions {
    packageMapping: ReadonlyMap<string, string>,
}


export async function emit(request: NobleIDLGenerationRequest<JSLanguageOptions>): Promise<NobleIDLGenerationResult> {
    const pkgMapping = getPackageMapping(request.languageOptions);

    const emitter = new PackageEmitter(request.model, pkgMapping, request.languageOptions.outputDir);
    await emitter.emitModules();

    return emitter.generationResult();
}


class PackageEmitter {
    constructor(
		private readonly model: NobleIDLModel,
		private readonly pkgMapping: ReadonlyMap<string, JSModule>,
		private readonly outputDir: string
	) {}

    readonly #outputFiles: string[] = [];


    async emitModules(): Promise<void> {
        const packageGroups = new Map<string, DefinitionInfo[]>();

        for(const def of this.model.definitions) {
            const pkgName = getPackageNameStr(def.name.package);
            let items = packageGroups.get(pkgName);
            if(items === undefined) {
                items = [];
                packageGroups.set(pkgName, items);
            }

            items.push(def);
        }

        for(const [pkg, defs] of packageGroups) {
            const packageName: PackageName = decodePackageNameStr(pkg);
            const p = await this.#emitModule(packageName, defs);
            this.#outputFiles.push(p);
        }
    }

    generationResult(): NobleIDLGenerationResult {
        return {
            generatedFiles: this.#outputFiles,
        };
    }

    // Returns the path of the file.
    async #emitModule(packageName: PackageName, definitions: readonly DefinitionInfo[]): Promise<string> {
		const modScanner = new ModuleScanner(packageName, definitions);
		modScanner.scanModule();

		const modEmitter = new ModEmitter(this.pkgMapping, this.outputDir, packageName, definitions, modScanner.metadata);
		return await modEmitter.emitModule();
    }

}


class ModEmitter {
    constructor(
        private readonly pkgMapping: ReadonlyMap<string, JSModule>,
		private readonly outputDir: string,
		private readonly currentPackage: PackageName,
		private readonly definitions: readonly DefinitionInfo[],
		private readonly metadata: ModuleMetadata,
    ) {}


	#getPackagePathPart(packageName: PackageName): string {
        const packageNameStr = getPackageNameStr(packageName);
        const mappedPackage = this.pkgMapping.get(packageNameStr);
        if(mappedPackage === undefined) {
            throw new Error("Unmapped package: " + packageNameStr);
        }

        if(mappedPackage.path === "") {
            return "index";
        }
        else if(mappedPackage.path.match(/^_*index$/)) {
            return "_" + mappedPackage.path;
        }
        else {
            return mappedPackage.path;
        }
	}

    #buildPackagePath(packageName: PackageName): string {
		const part = this.#getPackagePathPart(packageName);
		return path.join(this.outputDir, part + ".ts");
    }

    #getExternPath(): string {
		const packagePath = this.#getPackagePathPart(this.currentPackage);
		return "./" + posixPath.basename(packagePath) + ".extern.js";
    }

    #getImportPath(packageName: PackageName): string {
        const packageNameStr = getPackageNameStr(packageName);
        const mappedPackage = this.pkgMapping.get(packageNameStr);
        if(mappedPackage === undefined) {
            throw new Error("Unmapped package: " + packageNameStr);
        }

		if(mappedPackage.isCurrentPackage) {
			const fromPath = this.#getPackagePathPart(this.currentPackage) + ".js";
			const toPath = this.#getPackagePathPart(packageName) + ".js";
			return "./" + posixPath.relative(fromPath, toPath);
		}
        else if(mappedPackage.path === "") {
            return mappedPackage.packageName;
        }
        else {
            return mappedPackage.packageName + "/" + mappedPackage.path + ".js";
        }
    }

    // Returns the path of the file.
    async emitModule(): Promise<string> {
        const p = this.#buildPackagePath(this.currentPackage);

        await fs.mkdir(path.dirname(p), { recursive: true });

        const sourceFile = ts.createSourceFile(
            p,
            "",
            ts.ScriptTarget.Latest,
            false,
            ts.ScriptKind.TS,
        );

        const printer = ts.createPrinter();

        const file = await fs.open(p, "w");
        try {
			if(this.metadata.needsESExprImport) {
				const node = ts.factory.createImportDeclaration(
					undefined,
					ts.factory.createImportClause(
						false,
						undefined,
						ts.factory.createNamespaceImport(
							ts.factory.createIdentifier("$esexpr"),
						),
					),
					ts.factory.createStringLiteral("@argon-lang/esexpr"),
				);

				await file.write(printer.printNode(ts.EmitHint.Unspecified, node, sourceFile));
				await file.write(os.EOL);
			}

			for(const [pkg, refContext] of this.metadata.referencedPackages) {
				const node = ts.factory.createImportDeclaration(
					undefined,
					ts.factory.createImportClause(
						refContext.isTypeOnly,
						undefined,
						ts.factory.createNamespaceImport(
							ts.factory.createIdentifier(getPackageIdStr(pkg)),
						),
					),
					ts.factory.createStringLiteral(this.#getImportPath(decodePackageNameStr(pkg))),
				);

				await file.write(printer.printNode(ts.EmitHint.Unspecified, node, sourceFile));
				await file.write(os.EOL);
			}

			if(this.metadata.externTypes.size > 0) {
                const nodes = this.#emitExterns();
                for(const node of nodes) {
                    await file.write(printer.printNode(ts.EmitHint.Unspecified, node, sourceFile));
                    await file.write(os.EOL);
                }
			}

            for(const def of this.definitions) {
                const nodes = this.#emitDefinition(def);
                for(const node of nodes) {
                    await file.write(printer.printNode(ts.EmitHint.Unspecified, node, sourceFile));
                    await file.write(os.EOL);
                }
            }
        }
        finally {
            await file.close();
        }

        return p;
    }

	#emitExterns(): readonly ts.Node[] {
		const nodes: ts.Node[] = [];

		const externTypes = Array.from(this.metadata.externTypes);

		if(externTypes.some(([, et]) => et.isReferencedInModule)) {
			const allTypeOnlyImport = externTypes.every(([, et]) => !et.isReferencedInModule || et.isTypeOnly);

			nodes.push(ts.factory.createImportDeclaration(
				undefined,
				ts.factory.createImportClause(
					allTypeOnlyImport,
					undefined,
					ts.factory.createNamedImports(
						externTypes
							.filter(([ , et ]) => et.isReferencedInModule)
							.map(([ name, et ]) => ts.factory.createImportSpecifier(
								et.isTypeOnly && !allTypeOnlyImport,
								undefined,
								ts.factory.createIdentifier(convertIdPascal(name)),
							))
					),
				),
				ts.factory.createStringLiteral(this.#getExternPath()),
			));
		}

		if(externTypes.length >= 0) {
			const allTypeOnlyExport = externTypes.every(([, et]) => et.isTypeOnly);

			nodes.push(ts.factory.createExportDeclaration(
				undefined,
				allTypeOnlyExport,
				ts.factory.createNamedExports(
					externTypes
						.map(([ name, et ]) => ts.factory.createExportSpecifier(
							et.isTypeOnly && !allTypeOnlyExport,
							undefined,
							ts.factory.createIdentifier(convertIdPascal(name)),
						))
				),
				ts.factory.createStringLiteral(this.#getExternPath()),
				undefined,
			));
		}

		return nodes;
	}

    #emitDefinition(def: DefinitionInfo): ts.Node[] {
		let nodes: ts.Node[];
        switch(def.definition.$type) {
            case "record":
				nodes = this.#emitRecord(def, def.definition.record);
				break;

            case "enum":
				nodes = this.#emitEnum(def, def.definition.enum);
				break;

            case "extern-type":
				nodes = [];
				break;

            case "interface":
				nodes = this.#emitInterface(def, def.definition.interface);
				break;
        }

		if(this.metadata.shadowedTypes.has(def.name.name)) {
			nodes.push(ts.factory.createTypeAliasDeclaration(
				undefined,
				getUnshadowedName(def.name.name),
				this.#emitTypeParameters(def.typeParameters),
				ts.factory.createTypeReferenceNode(
					convertIdPascal(def.name.name),
					def.typeParameters.length === 0 ? undefined : def.typeParameters.map(arg =>
						ts.factory.createTypeReferenceNode(
							convertIdPascal(arg.name),
							undefined,
						)
					),
				),
			));
		}

		return nodes;
    }

    #emitRecord(def: DefinitionInfo, r: RecordDefinition): ts.Node[] {
		const nodes: ts.Node[] = [];

        nodes.push(ts.factory.createInterfaceDeclaration(
            [ ts.factory.createModifier(ts.SyntaxKind.ExportKeyword) ],
            convertIdPascal(def.name.name),
            this.#emitTypeParameters(def.typeParameters),
            undefined,
            r.fields.map(field => this.#emitField(field)),
        ));

		const deriveCodec = getESExprAnn(def.annotations, ESExprAnnRecord.codec, "derive-codec");
		if(deriveCodec !== null) {
			const recordConstructor = getESExprAnn(def.annotations, ESExprAnnRecord.codec, "constructor");

			const codecExpr = ts.factory.createCallExpression(
				ts.factory.createPropertyAccessExpression(
					ts.factory.createIdentifier("$esexpr"),
					"recordCodec",
				),
				[
					this.#emitTypeExpr(this.#defAsType(def)),
				],
				[
					ts.factory.createStringLiteral(recordConstructor?.name ?? def.name.name),
					this.#emitFieldCodecs(r.fields),
				],
			)

			nodes.push(ts.factory.createModuleDeclaration(
				[ ts.factory.createModifier(ts.SyntaxKind.ExportKeyword) ],
				ts.factory.createIdentifier(convertIdPascal(def.name.name)),
				ts.factory.createModuleBlock([
					this.#emitCodecDecl(def, codecExpr),
				]),
				ts.NodeFlags.Namespace,
			))
		}

        return nodes;
    }

    #emitEnum(def: DefinitionInfo, e: EnumDefinition): ts.Node[] {
		const nodes: ts.Node[] = [];

        nodes.push(ts.factory.createTypeAliasDeclaration(
            [ ts.factory.createModifier(ts.SyntaxKind.ExportKeyword) ],
            convertIdPascal(def.name.name),
            this.#emitTypeParameters(def.typeParameters),
            ts.factory.createUnionTypeNode(e.cases.map(c => this.#emitEnumCase(c))),
        ));

		const deriveCodec = getESExprAnn(def.annotations, ESExprAnnRecord.codec, "derive-codec");
		if(deriveCodec !== null) {
			if(getESExprAnn(def.annotations, ESExprAnnEnum.codec, "simple-enum") !== null) {
				console.log(def);
				throw new Error("Not implemented");
			}

			const codecExpr = ts.factory.createCallExpression(
				ts.factory.createPropertyAccessExpression(
					ts.factory.createIdentifier("$esexpr"),
					"enumCodec",
				),
				[
					this.#emitTypeExpr(this.#defAsType(def)),
				],
				[
					ts.factory.createObjectLiteralExpression(
						e.cases.map(c => {
							const caseConstructor = getESExprAnn(c.annotations, ESExprAnnEnumCase.codec, "constructor");

							let caseCodec: ts.Expression;

							if(getESExprAnn(c.annotations, ESExprAnnEnumCase.codec, "inline-value") !== null) {
								const field = c.fields[0];
								if(field === undefined || c.fields.length !== 1) {
									throw new Error("Inline value must have a single field");
								}

								caseCodec = ts.factory.createCallExpression(
									ts.factory.createPropertyAccessExpression(
										ts.factory.createIdentifier("$esexpr"),
										"inlineCaseCodec",
									),
									undefined,
									[
										ts.factory.createStringLiteral(field.name),
										this.#emitCodecExpr(field.fieldType),
									],
								);
							}
							else {
								caseCodec = ts.factory.createCallExpression(
									ts.factory.createPropertyAccessExpression(
										ts.factory.createIdentifier("$esexpr"),
										"caseCodec",
									),
									undefined,
									[ this.#emitFieldCodecs(c.fields) ],
								);
							}

							return ts.factory.createPropertyAssignment(
								ts.factory.createStringLiteral(caseConstructor?.name ?? c.name),
								caseCodec,
							);
						}),
						true,
					),
				],
			)

			nodes.push(ts.factory.createModuleDeclaration(
				[ ts.factory.createModifier(ts.SyntaxKind.ExportKeyword) ],
				ts.factory.createIdentifier(convertIdPascal(def.name.name)),
				ts.factory.createModuleBlock([
					this.#emitCodecDecl(def, codecExpr),
				]),
				ts.NodeFlags.Namespace,
			))
		}

        return nodes;
    }

    #emitEnumCase(c: EnumCase): ts.TypeNode {
		const caseConstructor = getESExprAnn(c.annotations, ESExprAnnEnumCase.codec, "constructor");

        return ts.factory.createTypeLiteralNode([
            ts.factory.createPropertySignature(
                undefined,
                "$type",
                undefined,
                ts.factory.createLiteralTypeNode(ts.factory.createStringLiteral(caseConstructor?.name ?? c.name)),
            ),

            ...c.fields.map(field => this.#emitField(field)),
        ]);
    }

    #emitInterface(def: DefinitionInfo, i: InterfaceDefinition): ts.Node[] {
        const iface = ts.factory.createInterfaceDeclaration(
            [ ts.factory.createModifier(ts.SyntaxKind.ExportKeyword) ],
            convertIdPascal(def.name.name),
            this.#emitTypeParameters(def.typeParameters),
            undefined,
            i.methods.map(m => this.#emitMethod(m)),
        );

        return [ iface ];
    }

    #emitTypeParameters(typeParameters: readonly TypeParameter[]): readonly ts.TypeParameterDeclaration[] | undefined {
        if(typeParameters.length === 0) {
            return undefined;
        }

        return typeParameters.map(tp => ts.factory.createTypeParameterDeclaration(
            undefined,
            convertIdPascal(tp.name),
            undefined,
        ));
    }

    #emitField(field: RecordField): ts.TypeElement {
        return ts.factory.createPropertySignature(
            undefined,
            convertIdCamel(field.name),
            undefined,
            this.#emitTypeExpr(field.fieldType),
        );
    }

	#emitFieldCodecs(fields: readonly RecordField[]): ts.Expression {
		return ts.factory.createObjectLiteralExpression(
			fields.map(f => {
				const fieldCodec: ts.Expression = (() => {
					if(getESExprAnn(f.annotations, ESExprAnnRecordField.codec, "vararg") !== null) {
						return ts.factory.createCallExpression(
							ts.factory.createPropertyAccessExpression(
								ts.factory.createIdentifier("$esexpr"),
								"varargFieldCodec",
							),
							undefined,
							[ this.#emitCodecExprImpl(f.fieldType, "varargCodec") ],
						);
					}

					if(getESExprAnn(f.annotations, ESExprAnnRecordField.codec, "dict") !== null) {
						return ts.factory.createCallExpression(
							ts.factory.createPropertyAccessExpression(
								ts.factory.createIdentifier("$esexpr"),
								"dictFieldCodec",
							),
							undefined,
							[ this.#emitCodecExprImpl(f.fieldType, "dictCodec") ],
						);
					}

					const keyword = getESExprAnn(f.annotations, ESExprAnnRecordField.codec, "keyword")
					if(keyword !== null) {
						const keywordName = keyword.name ?? f.name;

						if(keyword.defaultValue !== undefined) {
							throw new Error("Not implemented");
						}
						else if(!keyword.required) {
							return ts.factory.createCallExpression(
								ts.factory.createPropertyAccessExpression(
									ts.factory.createIdentifier("$esexpr"),
									"optionalKeywordFieldCodec",
								),
								undefined,
								[
									ts.factory.createStringLiteral(keywordName),
									this.#emitCodecExprImpl(f.fieldType, "optionalCodec"),
								],
							);
						}
						else {
							return ts.factory.createCallExpression(
								ts.factory.createPropertyAccessExpression(
									ts.factory.createIdentifier("$esexpr"),
									"keywordFieldCodec",
								),
								undefined,
								[
									ts.factory.createStringLiteral(keywordName),
									this.#emitCodecExpr(f.fieldType),
								],
							);
						}
					}

					return ts.factory.createCallExpression(
						ts.factory.createPropertyAccessExpression(
							ts.factory.createIdentifier("$esexpr"),
							"positionalFieldCodec",
						),
						undefined,
						[
							this.#emitCodecExpr(f.fieldType),
						],
					);
				})();


				return ts.factory.createPropertyAssignment(
					ts.factory.createStringLiteral(f.name),
					fieldCodec,
				);
			}),
			true,
		);
	}

    #emitMethod(method: InterfaceMethod): ts.TypeElement {
        return ts.factory.createMethodSignature(
            undefined,
            convertIdCamel(method.name),
            undefined,
            this.#emitTypeParameters(method.typeParameters),
            method.parameters.map(p => ts.factory.createParameterDeclaration(
                undefined,
                undefined,
                convertIdCamel(p.name),
                undefined,
                this.#emitTypeExpr(p.parameterType),
            )),
            this.#emitTypeExpr(method.returnType),
        );
    }

    #emitTypeExpr(t: TypeExpr): ts.TypeNode {
        return this.#emitTypeExprImpl(t, []);
    }

    #emitTypeExprImpl(t: TypeExpr, args: readonly TypeExpr[]): ts.TypeNode {
        switch(t.$type) {
            case "defined-type":
				if(PackageName.isSamePackage(this.currentPackage, t.name.package)) {
					if(this.metadata.shadowedTypes.has(t.name.name)) {
						return ts.factory.createTypeReferenceNode(
							getUnshadowedName(t.name.name),
							args.length === 0 ? undefined : args.map(arg => this.#emitTypeExpr(arg)),
						);
					}
					else {
						return ts.factory.createTypeReferenceNode(
							convertIdPascal(t.name.name),
							args.length === 0 ? undefined : args.map(arg => this.#emitTypeExpr(arg)),
						);
					}
				}
				else {
					return ts.factory.createTypeReferenceNode(
						ts.factory.createQualifiedName(
							ts.factory.createIdentifier(getPackageIdStr(t.name.package)),
							convertIdPascal(t.name.name)
						),
						args.length === 0 ? undefined : args.map(arg => this.#emitTypeExpr(arg)),
					);
				}

            case "type-parameter":
                return ts.factory.createTypeReferenceNode(
                    ts.factory.createIdentifier(convertIdPascal(t.name)),
                    args.length === 0 ? undefined : args.map(arg => this.#emitTypeExpr(arg)),
                );

            case "apply":
                return this.#emitTypeExprImpl(t.baseType, [...args, ...t.args])
        }
    }




	#emitCodecDecl(def: DefinitionInfo, codecExpr: ts.Expression): ts.Statement {
		const cachedCodec = ts.factory.createCallExpression(
			ts.factory.createPropertyAccessExpression(
				ts.factory.createIdentifier("$esexpr"),
				"lazyCodec",
			),
			undefined,
			[
				ts.factory.createArrowFunction(
					undefined,
					undefined,
					[],
					undefined,
					ts.factory.createToken(ts.SyntaxKind.EqualsGreaterThanToken),
					codecExpr,
				)
			],
		)

		if(def.typeParameters.length === 0) {
			return ts.factory.createVariableStatement(
				[
					ts.factory.createModifier(ts.SyntaxKind.ExportKeyword),
				],
				ts.factory.createVariableDeclarationList(
					[
						ts.factory.createVariableDeclaration(
							"codec",
							undefined,
							ts.factory.createTypeReferenceNode(
								ts.factory.createQualifiedName(
									ts.factory.createIdentifier("$esexpr"),
									"ESExprCodec"
								),
								[
									this.#emitTypeExpr(this.#defAsType(def)),
								]
							),
							cachedCodec,
						)
					],
					ts.NodeFlags.Const
				)
			);
		}
		else {
			return ts.factory.createFunctionDeclaration(
				[
					ts.factory.createModifier(ts.SyntaxKind.ExportKeyword),
					ts.factory.createModifier(ts.SyntaxKind.ConstKeyword),
				],
				undefined,
				"codec",
				this.#emitTypeParameters(def.typeParameters),
				def.typeParameters.map(tp => ts.factory.createParameterDeclaration(
					undefined,
					undefined,
					convertIdCamel(tp.name) + "Codec",
					undefined,
					ts.factory.createTypeReferenceNode(
						ts.factory.createQualifiedName(
							ts.factory.createIdentifier("$esexpr"),
							"ESExprCodec"
						),
						[
							this.#emitTypeExpr({ $type: "type-parameter", name: tp.name }),
						]
					),
					undefined,
				)),
				ts.factory.createTypeReferenceNode(
					ts.factory.createQualifiedName(
						ts.factory.createIdentifier("$esexpr"),
						"ESExprCodec"
					),
					[
						this.#emitTypeExpr(this.#defAsType(def)),
					]
				),
				ts.factory.createBlock([
					ts.factory.createReturnStatement(cachedCodec),
				]),
			)
		}
	}

	#emitCodecExpr(t: TypeExpr): ts.Expression {
		return this.#emitCodecExprImpl(t, "codec");
	}

	#emitCodecExprImpl(t: TypeExpr, codecMethod: string): ts.Expression {
		switch(t.$type) {
            case "defined-type":
			{
				let typeModule: ts.Expression;

				if(PackageName.isSamePackage(this.currentPackage, t.name.package)) {
					if(this.metadata.shadowedTypes.has(t.name.name)) {
						typeModule = ts.factory.createIdentifier(getUnshadowedName(t.name.name));
					}
					else {
						typeModule = ts.factory.createIdentifier(convertIdPascal(t.name.name));
					}
				}
				else {
					typeModule = ts.factory.createPropertyAccessExpression(
						ts.factory.createIdentifier(getPackageIdStr(t.name.package)),
						convertIdPascal(t.name.name)
					);
				}

				return ts.factory.createPropertyAccessExpression(typeModule, codecMethod);
			}

            case "type-parameter":
                return ts.factory.createIdentifier(convertIdCamel(t.name) + "Codec");

            case "apply":
                return ts.factory.createCallExpression(
					this.#emitCodecExprImpl(t.baseType, codecMethod),
					t.args.map(a => this.#emitTypeExpr(a)),
					t.args.map(a => this.#emitCodecExpr(a)),
				);
		}
	}

	#defAsType(def: DefinitionInfo): TypeExpr {
		const baseType: TypeExpr = {
			$type: "defined-type",
			name: def.name,
		};

		if(def.typeParameters.length === 0) {
			return baseType;
		}
		else {
			return {
				$type: "apply",
				baseType,
				args: def.typeParameters.map(tp => ({
					$type: "type-parameter",
					name: tp.name,
				}))
			}
		}
	}


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

interface ModuleMetadata {
	referencedPackages: Map<string, ReferencedPackageInfo>;
	externTypes: Map<string, ExternTypeInfo>;
	shadowedTypes: Set<string>;
	needsESExprImport: boolean,
}

class ModuleScanner {

	constructor(
		private currentPackage: PackageName,
		private definitions: readonly DefinitionInfo[],
	) {}

	metadata: ModuleMetadata = {
		referencedPackages: new Map(),
		externTypes: new Map(),
		shadowedTypes: new Set(),
		needsESExprImport: false,
	};


    scanModule(): void {
		for(const def of this.definitions) {
			if(def.definition.$type !== "extern-type") {
				continue;
			}
			this.#scanExtern(def);
		}
		for(const def of this.definitions) {
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
        switch(def.definition.$type) {
            case "record":
				this.#scanRecord(def, def.definition.record);
				break;

            case "enum":
				this.#scanEnum(def, def.definition.enum);
				break;

			// Extern types are scanned first
            case "extern-type":
				break;

            case "interface":
				this.#scanInterface(def, def.definition.interface);
				break;
        }
    }

    #scanRecord(def: DefinitionInfo, r: RecordDefinition): void {
		const hasESExpr = getESExprAnn(def.annotations, ESExprAnnRecord.codec, "derive-codec") !== null;

		const refContext: TypeReferenceContext = {
			isTypeOnly: !hasESExpr,
			typeParameters: this.#buildTypeParameters(def.typeParameters),
		};

		this.metadata.needsESExprImport ||= hasESExpr;


		for(const f of r.fields) {
			this.#scanField(f, refContext);
		}
    }

    #scanEnum(def: DefinitionInfo, e: EnumDefinition): void {
		let hasESExpr = getESExprAnn(def.annotations, ESExprAnnEnum.codec, "derive-codec") !== null;

		const refContext: TypeReferenceContext = {
			isTypeOnly: !hasESExpr,
			typeParameters: this.#buildTypeParameters(def.typeParameters),
		};


        for(const c of e.cases) {
			for(const f of c.fields) {
				this.#scanField(f, refContext);
			}
		}
    }

	#scanExtern(def: DefinitionInfo): void {
		let hasESExpr = getESExprAnn(def.annotations, ESExprAnnExternType.codec, "derive-codec") !== null;

		this.metadata.externTypes.set(def.name.name, {
			isReferencedInModule: false,
			isTypeOnly: !hasESExpr,
		});
	}

    #scanInterface(def: DefinitionInfo, i: InterfaceDefinition): void {
		const refContext: TypeReferenceContext = {
			isTypeOnly: true,
			typeParameters: this.#buildTypeParameters(def.typeParameters),
		};

		for(const m of i.methods) {
			this.#scanMethod(m, refContext);
		}
    }

    #scanField(field: RecordField, refContext: TypeReferenceContext): void {
        this.#scanType(field.fieldType, refContext);
    }

    #scanMethod(method: InterfaceMethod, refContext: TypeReferenceContext): void {
		const refContext2: TypeReferenceContext = {
			...refContext,
			typeParameters: this.#buildTypeParameters(method.typeParameters, refContext.typeParameters),
		};

		for(const p of method.parameters) {
			this.#scanType(p.parameterType, refContext2);
		}
		this.#scanType(method.returnType, refContext2);
    }

	#scanType(t: TypeExpr, refContext: TypeReferenceContext): void {
		switch(t.$type) {
			case "defined-type":
				if(PackageName.isSamePackage(t.name.package, this.currentPackage)) {
					const externInfo = this.metadata.externTypes.get(t.name.name);
					if(externInfo !== undefined) {
						externInfo.isReferencedInModule = true;
					}

					if(refContext.typeParameters.has(t.name.name)) {
						this.metadata.shadowedTypes.add(t.name.name);
					}
				}
				else {
					this.#addPackage(t.name.package, refContext);
				}
				break;

			case "type-parameter":
				break;

			case "apply":
				this.#scanType(t.baseType, refContext);
				for(const arg of t.args) {
					this.#scanType(arg, refContext);
				}
				break;
		}
	}

	#buildTypeParameters(typeParams: readonly TypeParameter[], existing?: ReadonlySet<string>): Set<string> {
		const params = new Set(existing);
		for(const tp of typeParams) {
			params.add(tp.name);
		}
		return params;
	}
}



interface JSModule {
	isCurrentPackage: boolean;
    packageName: string;
    path: string;
}


function convertIdPascal(kebab: string): string {
    return kebab
        .split('-')
        .map(segment => (segment.match(/^\d/) ? "-" : "") + segment)
        .map(segment => segment.charAt(0).toUpperCase() + segment.slice(1))
        .join('');
}

function convertIdCamel(kebab: string): string {
    const pascalCase = convertIdPascal(kebab);
    return pascalCase.charAt(0).toLowerCase() + pascalCase.slice(1);
}

function getUnshadowedName(name: string): string {
	return "$Unshadowed_" + convertIdPascal(name);
}



function getPackageNameStr(name: PackageName): string {
    return name.parts.join(".");
}

function decodePackageNameStr(name: string): PackageName {
	return {
		parts: name.length === 0 ? [] : name.split("."),
	};
}

function getPackageIdStr(name: PackageName | string): string {
	if(typeof name === "object") {
		return getPackageIdStr(getPackageNameStr(name));
	}

	return name.replace(".", "__").replace("-", "_");
}


function getPackageMapping(options: JSLanguageOptions): Map<string, JSModule> {
    let pkgMapping = new Map<string, JSModule>();

	for(const [packageName, packageOptions] of options.packageOptions) {
		const isCurrentPackage = options.packageName == packageName;

        for(const [idlPackage, jsPath] of packageOptions.packageMapping) {
            pkgMapping.set(idlPackage, {
				isCurrentPackage,
                packageName: packageName,
                path: jsPath,
            });
        }
    }

    return pkgMapping;
}


function getESExprAnn<A extends { readonly $type: string }, Name extends A["$type"]>(annotations: readonly Annotation[], codec: ESExprCodec<A>, name: Name): (A & { readonly $type: Name }) | null {
	for(const ann of annotations) {
		if(ann.scope !== "esexpr") {
			continue;
		}

		const valueRes = codec.decode(ann.value);
		if(!valueRes.success) {
			throw new Error("Invalid ESExpr annotation");
		}

		const value = valueRes.value;
		if(value.$type === name) {
			return value as A & { readonly $type: Name };
		}
	}

	return null;
}


