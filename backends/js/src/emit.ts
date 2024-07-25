import type { DefinitionInfo, EnumCase, EnumDefinition, InterfaceDefinition, InterfaceMethod, NobleIDLGenerationRequest, NobleIDLGenerationResult, NobleIDLModel, PackageName, RecordDefinition, RecordField, TypeExpr, TypeParameter } from "./api.js";

import * as path from "node:path";
import * as fs from "node:fs/promises";
import * as os from "node:os";
import * as ts from "typescript";

export interface JSLanguageOptions {
    // The name of the NPM package (in package.json)
    packageName: string,

    // The directory where files will be written.
    outputDir: string,

    packageOptions: Map<string, PackageOptions>,
}

export interface PackageOptions {
    packageMapping: Map<string, string>,
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

    #buildPackagePath(packageName: PackageName): string {
        const packageNameStr = getPackageNameStr(packageName);
        const mappedPackage = this.#pkgMapping.get(packageNameStr);
        if(mappedPackage === undefined) {
            throw new Error("Unmapped package: " + packageNameStr);
        }

        if(mappedPackage.path === "") {
            return path.join(this.#outputDir, "index.ts");
        }
        else if(mappedPackage.path.match(/^_*index$/)) {
            return path.join(this.#outputDir, "_" + mappedPackage.path + ".ts");
        }
        else {
            return path.join(this.#outputDir, mappedPackage.path + ".ts");
        }
    }

    #getExternPath(packageName: PackageName): string {
        if(packageName.parts.length === 0) {
            return "./index.extern.js";
        }
        else {
            const part = packageName.parts[packageName.parts.length - 1]!;
            return `./${part}.extern.js`;
        }
    }

    #getImportPath(packageName: PackageName): string {
        const packageNameStr = getPackageNameStr(packageName);
        const mappedPackage = this.#pkgMapping.get(packageNameStr);
        if(mappedPackage === undefined) {
            throw new Error("Unmapped package: " + packageNameStr);
        }

        if(mappedPackage.path === "") {
            return mappedPackage.packageName;
        }
        else if(mappedPackage.path.match(/^_*index$/)) {
            return mappedPackage.packageName + "/_" + mappedPackage.path + ".js";
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
            r.fields.map(field => this.#emitField(field)),
        );

        return [ recIface ];
    }

    #emitEnum(def: DefinitionInfo, e: EnumDefinition): readonly ts.Node[] {
        const enumType = ts.factory.createTypeAliasDeclaration(
            [ ts.factory.createModifier(ts.SyntaxKind.ExportKeyword) ],
            convertIdPascal(def.name.name),
            this.#emitTypeParameters(def.typeParameters),
            ts.factory.createUnionTypeNode(e.cases.map(c => this.#emitEnumCase(c))),
        );

        return [ enumType ];
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
                return ts.factory.createImportTypeNode(
                    ts.factory.createLiteralTypeNode(ts.factory.createStringLiteral(this.#getImportPath(t.name.package))),
                    undefined,
                    ts.factory.createIdentifier(convertIdPascal(t.name.name)),
                    args.length === 0 ? undefined : args.map(arg => this.#emitTypeExpr(arg)),
                );

            case "type-parameter":
                return ts.factory.createTypeReferenceNode(
                    ts.factory.createIdentifier(convertIdPascal(t.name)),
                    args.length === 0 ? undefined : args.map(arg => this.#emitTypeExpr(arg)),
                );

            case "apply":
                return this.#emitTypeExprImpl(t.baseType, [...args, ...t.args])
        }
    }



}


interface JSModule {
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
        for(const [idlPackage, jsPath] of packageOptions.packageMapping) {
            pkgMapping.set(idlPackage, {
                packageName: packageName,
                path: jsPath,
            });
        }
    }

    return pkgMapping;
}

