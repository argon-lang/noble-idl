import { type DefinitionInfo, type EnumCase, type EnumDefinition, type InterfaceDefinition, type InterfaceMethod, type NobleIDLGenerationRequest, type NobleIDLGenerationResult, type NobleIDLModel, PackageName, type RecordDefinition, type RecordField, type TypeExpr, type TypeParameter, ExternTypeDefinition, ESExprDecodedValue, QualifiedName } from "./api.js";

import * as path from "node:path";
import * as posixPath from "node:path/posix";
import * as fs from "node:fs/promises";
import * as os from "node:os";
import * as ts from "typescript";

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
			if(def.isLibrary) {
				continue;
			}

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

		if(r.esexprOptions !== undefined) {
			const codecExpr = ts.factory.createCallExpression(
				ts.factory.createPropertyAccessExpression(
					ts.factory.createIdentifier("$esexpr"),
					"recordCodec",
				),
				[
					this.#emitTypeExpr(this.#defAsType(def)),
				],
				[
					ts.factory.createStringLiteral(r.esexprOptions.constructor),
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

		if(e.esexprOptions !== undefined) {
			if(e.esexprOptions.simpleEnum) {
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
							if(c.esexprOptions === undefined) {
								throw new Error("Missing esexpr-options for case of enum with esexpr-options");
							}

							let caseCodec: ts.Expression;

							if(c.esexprOptions.caseType.$type === "inline-value") {
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
									[
										ts.factory.createStringLiteral(c.esexprOptions.caseType.name),
										this.#emitFieldCodecs(c.fields),
									],
								);
							}

							return ts.factory.createPropertyAssignment(
								ts.factory.createStringLiteral(c.name),
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
        return ts.factory.createTypeLiteralNode([
            ts.factory.createPropertySignature(
                undefined,
                "$type",
                undefined,
                ts.factory.createLiteralTypeNode(ts.factory.createStringLiteral(c.name)),
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
				if(f.esexprOptions === undefined) {
					throw new Error("Field defined in type with ESExpr codec is missing esexpr-options");
				}

				const fieldCodec: ts.Expression = (() => {
					switch(f.esexprOptions.kind.$type) {
						case "vararg":
							return ts.factory.createCallExpression(
								ts.factory.createPropertyAccessExpression(
									ts.factory.createIdentifier("$esexpr"),
									"varargFieldCodec",
								),
								undefined,
								[ this.#emitCodecExprImpl(f.fieldType, "varargCodec") ],
							);

						case "dict":
							return ts.factory.createCallExpression(
								ts.factory.createPropertyAccessExpression(
									ts.factory.createIdentifier("$esexpr"),
									"dictFieldCodec",
								),
								undefined,
								[ this.#emitCodecExprImpl(f.fieldType, "dictCodec") ],
							);

						case "keyword":
						{
							const keywordName = f.esexprOptions.kind.name;

							switch(f.esexprOptions.kind.mode.$type) {
								case "default-value":
									return ts.factory.createCallExpression(
										ts.factory.createPropertyAccessExpression(
											ts.factory.createIdentifier("$esexpr"),
											"defaultKeywordFieldCodec",
										),
										undefined,
										[
											ts.factory.createStringLiteral(keywordName),
											ts.factory.createArrowFunction(
												undefined,
												undefined,
												[],
												undefined,
												ts.factory.createToken(ts.SyntaxKind.EqualsGreaterThanToken),
												this.#emitValue(f.esexprOptions.kind.mode.defaultValue),
											),
											this.#emitCodecExpr(f.fieldType),
										],
									);


								case "optional":
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

								case "required":
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

						case "positional":
							switch(f.esexprOptions.kind.mode.$type) {
								case "optional":
									return ts.factory.createCallExpression(
										ts.factory.createPropertyAccessExpression(
											ts.factory.createIdentifier("$esexpr"),
											"optionalPositionalFieldCodec",
										),
										undefined,
										[
											this.#emitCodecExprImpl(f.fieldType, "optionalCodec"),
										],
									);

								case "required":
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
							}
					}
				})();


				return ts.factory.createPropertyAssignment(
					ts.factory.createStringLiteral(convertIdCamel(f.name)),
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
        switch(t.$type) {
            case "defined-type":
				if(PackageName.isSamePackage(this.currentPackage, t.name.package)) {
					if(this.metadata.shadowedTypes.has(t.name.name)) {
						return ts.factory.createTypeReferenceNode(
							getUnshadowedName(t.name.name),
							t.args.length === 0 ? undefined : t.args.map(arg => this.#emitTypeExpr(arg)),
						);
					}
					else {
						return ts.factory.createTypeReferenceNode(
							convertIdPascal(t.name.name),
							t.args.length === 0 ? undefined : t.args.map(arg => this.#emitTypeExpr(arg)),
						);
					}
				}
				else {
					return ts.factory.createTypeReferenceNode(
						ts.factory.createQualifiedName(
							ts.factory.createIdentifier(getPackageIdStr(t.name.package)),
							convertIdPascal(t.name.name)
						),
						t.args.length === 0 ? undefined : t.args.map(arg => this.#emitTypeExpr(arg)),
					);
				}

            case "type-parameter":
                return ts.factory.createTypeReferenceNode(
                    ts.factory.createIdentifier(convertIdPascal(t.name)),
                    undefined,
                );
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
				const typeModule: ts.Expression = this.#emitTypeModule(t.name);

				const codecExpr = ts.factory.createPropertyAccessExpression(typeModule, codecMethod);

				if(t.args.length === 0) {
					return codecExpr;
				}
				else {
					return ts.factory.createCallExpression(
						codecExpr,
						t.args.map(a => this.#emitTypeExpr(a)),
						t.args.map(a => this.#emitCodecExpr(a)),
					);
				}
			}

            case "type-parameter":
                return ts.factory.createIdentifier(convertIdCamel(t.name) + "Codec");
		}
	}

	#emitTypeModule(name: QualifiedName): ts.Expression {
		if(PackageName.isSamePackage(this.currentPackage, name.package)) {
			if(this.metadata.shadowedTypes.has(name.name)) {
				return ts.factory.createIdentifier(getUnshadowedName(name.name));
			}
			else {
				return ts.factory.createIdentifier(convertIdPascal(name.name));
			}
		}
		else {
			return ts.factory.createPropertyAccessExpression(
				ts.factory.createIdentifier(getPackageIdStr(name.package)),
				convertIdPascal(name.name)
			);
		}
	}

	#defAsType(def: DefinitionInfo): TypeExpr {
		return {
			$type: "defined-type",
			name: def.name,
			args: def.typeParameters.map(tp => ({
				$type: "type-parameter",
				name: tp.name,
			})),
		};
	}


	#emitValue(value: ESExprDecodedValue): ts.Expression {
		if(value.t.$type === "type-parameter") {
			throw new Error("Emitted values cannot have an unsubstituted type parameter for a value.");
		}

		const t = value.t;

		const typeArgs = () => t.args.length === 0
			? undefined
			: t.args.map(arg => this.#emitTypeExpr(arg));

		const fromValue = (funcName: string, value: ts.Expression): ts.Expression => {
			return ts.factory.createCallExpression(
				ts.factory.createPropertyAccessExpression(
					this.#emitTypeModule(t.name),
					funcName,
				),
				typeArgs(),
				[
					value,
				],
			);
		};

		switch(value.$type) {
			case "record":
				return ts.factory.createObjectLiteralExpression(
					Array.from(value.fieldValues.entries())
						.map(([ name, value ]) => ts.factory.createPropertyAssignment(
							convertIdCamel(name),
							this.#emitValue(value),
						)),
					true,
				);

			case "enum":
				return ts.factory.createObjectLiteralExpression(
					[
						ts.factory.createPropertyAssignment(ts.factory.createStringLiteral("$type"), ts.factory.createStringLiteral(value.caseName)),
						...Array.from(value.fieldValues.entries())
							.map(([ name, value ]) => ts.factory.createPropertyAssignment(
								convertIdCamel(name),
								this.#emitValue(value),
							)),
					],
					true,
				);

			case "optional":
			{
				if(value.value === undefined) {
					return ts.factory.createCallExpression(
						ts.factory.createPropertyAccessExpression(
							this.#emitTypeModule(value.t.name),
							"empty",
						),
						typeArgs(),
						[],
					);
				}
				else {
					return ts.factory.createCallExpression(
						ts.factory.createPropertyAccessExpression(
							this.#emitTypeModule(value.t.name),
							"fromElement",
						),
						typeArgs(),
						[
							this.#emitValue(value.value),
						],
					);
				}
			}

			case "vararg":
			{
				return ts.factory.createCallExpression(
					ts.factory.createPropertyAccessExpression(
						this.#emitTypeModule(value.t.name),
						"fromArray",
					),
					typeArgs(),
					[
						ts.factory.createArrayLiteralExpression(
							value.values.map(v => this.#emitValue(v)),
							true,
						),
					],
				);
			}

			case "dict":
			{
				return ts.factory.createCallExpression(
					ts.factory.createPropertyAccessExpression(
						this.#emitTypeModule(value.t.name),
						"fromMap",
					),
					typeArgs(),
					[
						ts.factory.createNewExpression(
							ts.factory.createIdentifier("Map"),
							[
								ts.factory.createKeywordTypeNode(ts.SyntaxKind.StringKeyword),
								this.#emitTypeExpr(value.elementType),
							],
							[
								ts.factory.createArrayLiteralExpression(
									Array.from(value.values.entries()).map(([k, v]) =>
										ts.factory.createArrayLiteralExpression(
											[
												ts.factory.createStringLiteral(k),
												this.#emitValue(v),
											],
											false,
										),
									),
									true,
								),
							],
						),
					],
				);
			}

			case "build-from":
				return fromValue("buildFrom", this.#emitValue(value.value));

			case "from-bool":
				return fromValue("fromBoolean", value.b ? ts.factory.createTrue() : ts.factory.createFalse());

			case "from-int":
				if(
					value.minInt !== undefined &&
					value.minInt >= BigInt(Number.MIN_SAFE_INTEGER) &&
					value.maxInt !== undefined &&
					value.maxInt <= BigInt(Number.MAX_SAFE_INTEGER)
				) {
					return fromValue("fromNumberInteger", ts.factory.createNumericLiteral(Number(value.i)));
				}
				else {
					return fromValue("fromBigInt", ts.factory.createBigIntLiteral(value.i.toString() + "n"));
				}

			case "from-str":
				return fromValue("fromString", ts.factory.createStringLiteral(value.s));

			case "from-binary":
				return fromValue("fromUint8Array", ts.factory.createNewExpression(
					ts.factory.createIdentifier("Uint8Array"),
					undefined,
					[
						ts.factory.createArrayLiteralExpression(
							Array.from(value.b).map(b => ts.factory.createNumericLiteral(b)),
							false,
						),
					],
				));

			case "from-float32":
				return fromValue("fromNumberFloat32", ts.factory.createNumericLiteral(value.f));

			case "from-float64":
				return fromValue("fromNumberFloat64", ts.factory.createNumericLiteral(value.f));

			case "from-null":
				return ts.factory.createCallExpression(
					ts.factory.createPropertyAccessExpression(
						this.#emitTypeModule(t.name),
						"fromNull",
					),
					typeArgs(),
					[],
				);


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
			this.#scanExtern(def, def.definition.ext);
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
		const hasESExpr = r.esexprOptions !== undefined;

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
		let hasESExpr = e.esexprOptions !== undefined;

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

	#scanExtern(def: DefinitionInfo, ext: ExternTypeDefinition): void {
		let hasESExpr = ext.esexprOptions !== undefined;

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

				for(const arg of t.args) {
					this.#scanType(arg, refContext);
				}
				break;

			case "type-parameter":
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



