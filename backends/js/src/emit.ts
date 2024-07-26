import type { DefinitionInfo, EnumCase, EnumDefinition, InterfaceDefinition, InterfaceMethod, NobleIDLGenerationRequest, NobleIDLGenerationResult, NobleIDLModel, PackageName, RecordDefinition, RecordField, TypeExpr, TypeParameter } from "./api.js";

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

    const emitter = new ModEmitter({
        model: request.model,
        pkgMapping,
        outputDir: request.languageOptions.outputDir,
    });

    await emitter.emitModules();

    return emitter.generationResult();
}

class ModEmitter {
    constructor(opts: {
        model: NobleIDLModel,
        pkgMapping: ReadonlyMap<string, JSModule>,
        outputDir: string,
    }) {
        this.#model = opts.model;
        this.#pkgMapping = opts.pkgMapping;
        this.#outputDir = opts.outputDir;
        this.#outputFiles = [];
    }

    readonly #model: NobleIDLModel;
    readonly #pkgMapping: ReadonlyMap<string, JSModule>;
    readonly #outputDir: string;
    readonly #outputFiles: string[];


    async emitModules(): Promise<void> {
        const packageGroups = new Map<string, DefinitionInfo[]>();

        for(const def of this.#model.definitions) {
            const pkgName = getPackageNameStr(def.name.package);
            let items = packageGroups.get(pkgName);
            if(items === undefined) {
                items = [];
                packageGroups.set(pkgName, items);
            }

            items.push(def);
        }

        for(const [pkg, defs] of packageGroups) {
            const packageName: PackageName = {
                parts: pkg.length === 0 ? [] : pkg.split("."),
            };
            const p = await this.#emitModule(packageName, defs);
            this.#outputFiles.push(p);
        }
    }

    generationResult(): NobleIDLGenerationResult {
        return {
            generatedFiles: this.#outputFiles,
        };
    }

	#getPackagePathPart(packageName: PackageName): string {
        const packageNameStr = getPackageNameStr(packageName);
        const mappedPackage = this.#pkgMapping.get(packageNameStr);
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
		return path.join(this.#outputDir, part + ".ts");
    }

    #getExternPath(packageName: PackageName): string {
		const packagePath = this.#getPackagePathPart(packageName);
		return "./" + posixPath.basename(packagePath) + ".extern.js";
    }

    #getImportPath(currentPackage: PackageName, packageName: PackageName): string {
        const packageNameStr = getPackageNameStr(packageName);
        const mappedPackage = this.#pkgMapping.get(packageNameStr);
        if(mappedPackage === undefined) {
            throw new Error("Unmapped package: " + packageNameStr);
        }

		if(mappedPackage.isCurrentPackage) {
			const fromPath = this.#getPackagePathPart(currentPackage) + ".js";
			const toPath = this.#getPackagePathPart(packageName) + ".js";

			if(fromPath === toPath) {
				return "./" + posixPath.basename(toPath);
			}

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
    async #emitModule(packageName: PackageName, definitions: readonly DefinitionInfo[]): Promise<string> {
        const p = this.#buildPackagePath(packageName);

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
            for(const def of definitions) {
                const nodes = await this.#emitDefinition(def);
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

    #emitDefinition(def: DefinitionInfo): readonly ts.Node[] {
        switch(def.definition.$type) {
            case "record": return this.#emitRecord(def, def.definition.record);
            case "enum": return this.#emitEnum(def, def.definition.enum);
            case "extern-type": return this.#emitExternType(def);
            case "interface": return this.#emitInterface (def, def.definition.interface);
        }
    }

    #emitRecord(def: DefinitionInfo, r: RecordDefinition): readonly ts.Node[] {
        const recIface = ts.factory.createInterfaceDeclaration(
            [ ts.factory.createModifier(ts.SyntaxKind.ExportKeyword) ],
            convertIdPascal(def.name.name),
            this.#emitTypeParameters(def.typeParameters),
            undefined,
            r.fields.map(field => this.#emitField(def.name.package, field)),
        );

        return [ recIface ];
    }

    #emitEnum(def: DefinitionInfo, e: EnumDefinition): readonly ts.Node[] {
        const enumType = ts.factory.createTypeAliasDeclaration(
            [ ts.factory.createModifier(ts.SyntaxKind.ExportKeyword) ],
            convertIdPascal(def.name.name),
            this.#emitTypeParameters(def.typeParameters),
            ts.factory.createUnionTypeNode(e.cases.map(c => this.#emitEnumCase(def.name.package, c))),
        );

        return [ enumType ];
    }

    #emitEnumCase(currentPackage: PackageName, c: EnumCase): ts.TypeNode {
        return ts.factory.createTypeLiteralNode([
            ts.factory.createPropertySignature(
                undefined,
                "$type",
                undefined,
                ts.factory.createLiteralTypeNode(ts.factory.createStringLiteral(c.name)),
            ),

            ...c.fields.map(field => this.#emitField(currentPackage, field)),
        ]);
    }

    #emitExternType(def: DefinitionInfo): readonly ts.Node[] {
        const importNode = ts.factory.createImportDeclaration(
            undefined,
            ts.factory.createImportClause(
                true,
                undefined,
                ts.factory.createNamedImports([
                    ts.factory.createImportSpecifier(
                        false,
                        undefined,
                        ts.factory.createIdentifier(convertIdPascal(def.name.name)),
                    ),
                ]),
            ),
            ts.factory.createStringLiteral(this.#getExternPath(def.name.package)),
            undefined,
        );

        const exportNode = ts.factory.createExportDeclaration(
            undefined,
            true,
            ts.factory.createNamedExports([
                ts.factory.createExportSpecifier(
                    false,
                    undefined,
                    convertIdPascal(def.name.name)
                ),
            ]),
            undefined,
            undefined,
        );

        return [importNode, exportNode];
    }

    #emitInterface(def: DefinitionInfo, i: InterfaceDefinition): readonly ts.Node[] {
        const iface = ts.factory.createInterfaceDeclaration(
            [ ts.factory.createModifier(ts.SyntaxKind.ExportKeyword) ],
            convertIdPascal(def.name.name),
            this.#emitTypeParameters(def.typeParameters),
            undefined,
            i.methods.map(m => this.#emitMethod(def.name.package, m)),
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

    #emitField(currentPackage: PackageName, field: RecordField): ts.TypeElement {
        return ts.factory.createPropertySignature(
            undefined,
            convertIdCamel(field.name),
            undefined,
            this.#emitTypeExpr(currentPackage, field.fieldType),
        );
    }

    #emitMethod(currentPackage: PackageName, method: InterfaceMethod): ts.TypeElement {
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
                this.#emitTypeExpr(currentPackage, p.parameterType),
            )),
            this.#emitTypeExpr(currentPackage, method.returnType),
        );
    }

    #emitTypeExpr(currentPackage: PackageName, t: TypeExpr): ts.TypeNode {
        return this.#emitTypeExprImpl(currentPackage, t, []);
    }

    #emitTypeExprImpl(currentPackage: PackageName, t: TypeExpr, args: readonly TypeExpr[]): ts.TypeNode {
        switch(t.$type) {
            case "defined-type":
                return ts.factory.createImportTypeNode(
                    ts.factory.createLiteralTypeNode(ts.factory.createStringLiteral(this.#getImportPath(currentPackage, t.name.package))),
                    undefined,
                    ts.factory.createIdentifier(convertIdPascal(t.name.name)),
                    args.length === 0 ? undefined : args.map(arg => this.#emitTypeExpr(currentPackage, arg)),
                );

            case "type-parameter":
                return ts.factory.createTypeReferenceNode(
                    ts.factory.createIdentifier(convertIdPascal(t.name)),
                    args.length === 0 ? undefined : args.map(arg => this.#emitTypeExpr(currentPackage, arg)),
                );

            case "apply":
                return this.#emitTypeExprImpl(currentPackage, t.baseType, [...args, ...t.args])
        }
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



function getPackageNameStr(name: PackageName): string {
    return name.parts.join(".");
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

