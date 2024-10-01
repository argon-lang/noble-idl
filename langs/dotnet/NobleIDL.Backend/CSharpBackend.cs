using System.Text;
using ESExpr.Runtime;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.CSharp.Syntax;
using NobleIDL.Backend.Api;
using static Microsoft.CodeAnalysis.CSharp.SyntaxFactory;
using TypeParameter = NobleIDL.Backend.Api.TypeParameter;

namespace NobleIDL.Backend;

internal class CSharpBackend {
    public CSharpBackend(NobleIdlGenerationRequest<CSharpLanguageOptions> request, IReadOnlyList<string> inputSources) {
        this.request = request;
        this.inputSources = inputSources;
        namespaceMapping = GetNamespaceMapping(request.LanguageOptions);
    }
    
    private readonly NobleIdlGenerationRequest<CSharpLanguageOptions> request;
    private readonly IReadOnlyList<string> inputSources;
    private readonly Dictionary<PackageName, NameSyntax?> namespaceMapping;
    
    private NobleIdlModel Model => request.Model;

    public NobleIdlGenerationResult Result => new NobleIdlGenerationResult {
        GeneratedFiles = [ request.LanguageOptions.OutputFile ],
    };

    public async Task Emit(CancellationToken cancellationToken = default) {
        var file = EmitImpl(cancellationToken).NormalizeWhitespace();
        await File.WriteAllTextAsync(
            request.LanguageOptions.OutputFile,
            file.GetText(Encoding.UTF8).ToString(),
            Encoding.UTF8,
            cancellationToken
        ).ConfigureAwait(false);
    }


    private static Dictionary<PackageName, NameSyntax?> GetNamespaceMapping(CSharpLanguageOptions options) {
        return options.NamespaceMapping.Mapping.ToDictionary(
            kvp => PackageName.FromString(kvp.Key),
            kvp => {
                if(kvp.Value.Length == 0) {
                    return null;
                }
                
                var parts = kvp.Value.Split('.');

                NameSyntax name = AliasQualifiedName(
                    IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                    IdentifierName(parts[0])
                );

                for(int i = 1; i < parts.Length; ++i) {
                    name = QualifiedName(name, IdentifierName(parts[i]));
                }

                return name;
            }
        );
    }

    private NameSyntax? GetNamespace(PackageName packageName) {
        if(!namespaceMapping.TryGetValue(packageName, out var ns)) {
            throw new NobleIDLCompileErrorException("Unmapped package: " + packageName);
        }

        return ns;
    }

    private NameSyntax GetNamespaceMember(PackageName packageName, SimpleNameSyntax member) {
        var ns = GetNamespace(packageName);
        if(ns is null) {
            return AliasQualifiedName(
                IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                member
            );
        }
        else {
            return QualifiedName(ns, member);
        }
    }

    private NameSyntax RemoveGlobalPrefix(NameSyntax name) {
        return name switch {
            AliasQualifiedNameSyntax aliasQualifiedName => aliasQualifiedName.Name,
            
            QualifiedNameSyntax qualifiedName =>
                QualifiedName(RemoveGlobalPrefix(qualifiedName.Left), qualifiedName.Right),
            
            SimpleNameSyntax simpleName => simpleName,
            
            _ => throw new InvalidOperationException(),
        };
    }

    private CompilationUnitSyntax EmitImpl(CancellationToken cancellationToken) {
        var file = CompilationUnit();

        foreach(var inputSource in inputSources) {
            var attr =
                Attribute(
                    QualifiedName(
                        QualifiedName(
                            AliasQualifiedName(
                                IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                                IdentifierName("NobleIDL")
                            ),
                            IdentifierName("Runtime")
                        ),
                        IdentifierName("NobleIDLSourceFile")
                    )
                )
                    .AddArgumentListArguments(
                        AttributeArgument(LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(inputSource)))
                    );
            
            var attrList = AttributeList(SingletonSeparatedList(attr))
                .WithTarget(AttributeTargetSpecifier(Token(SyntaxKind.AssemblyKeyword)));
            
            file = file.AddAttributeLists(attrList);
        }

        var groups = Model.Definitions
            .Where(dfn => !dfn.IsLibrary)
            .GroupBy(dfn => dfn.Name.Package)
            .OrderBy(group => group.Key)
            .Select(group => (group.Key, group.ToList()))
            .ToList();
        
        foreach(var (package, dfns) in groups) {
            cancellationToken.ThrowIfCancellationRequested();

            var ns = GetNamespace(package);
            string namespaceStr;
            if(ns is null) {
                namespaceStr = "";
            }
            else {
                namespaceStr = RemoveGlobalPrefix(ns).ToString();
            }
            
            var attr =
                Attribute(
                        QualifiedName(
                            QualifiedName(
                                AliasQualifiedName(
                                    IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                                    IdentifierName("NobleIDL")
                                ),
                                IdentifierName("Runtime")
                            ),
                            IdentifierName("NobleIDLPackageMapping")
                        )
                    )
                    .AddArgumentListArguments(
                        AttributeArgument(LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(package.ToString()))),
                        AttributeArgument(LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(namespaceStr)))
                    );
            
            var attrList = AttributeList(SingletonSeparatedList(attr))
                .WithTarget(AttributeTargetSpecifier(Token(SyntaxKind.AssemblyKeyword)));

            file = file.AddAttributeLists(attrList);
            
            file = file.AddMembers(EmitNamespace(package, dfns, cancellationToken));
        }

        return file;
    }

    private MemberDeclarationSyntax[] EmitNamespace(PackageName package, List<DefinitionInfo> dfns, CancellationToken cancellationToken) {
        var ns = GetNamespace(package);

        var nsMembers = dfns
            .SelectMany(dfn => {
                cancellationToken.ThrowIfCancellationRequested();
                return EmitDefinition(dfn);
            })
            .ToArray();
        
        if(ns is null) {
            return nsMembers;
        }
        else {
            return [ NamespaceDeclaration(RemoveGlobalPrefix(ns)).AddMembers(nsMembers) ];
        }
    }

    private MemberDeclarationSyntax[] EmitDefinition(DefinitionInfo dfn) =>
        dfn.Definition switch {
            Definition.Record { R: var r } => [EmitRecordDefinition(dfn, r)],
            Definition.Enum { E: var e } => [EmitEnumDefinition(dfn, e)],
            Definition.SimpleEnum { E: var e } => [EmitSimpleDefinition(dfn, e)],
            Definition.ExternType => [],
            Definition.Interface { Iface: var iface } => [EmitInterfaceDefinition(dfn, iface)],
            Definition.ExceptionType { Ex: var ex } => [EmitExceptionTypeDefinition(dfn, ex)],
            _ => throw new InvalidOperationException(),
        };


    private MemberDeclarationSyntax EmitRecordDefinition(DefinitionInfo dfn, RecordDefinition r) {
        var rec = RecordDeclaration(Token(SyntaxKind.RecordKeyword), ConvertIdPascal(dfn.Name.Name))
            .WithOpenBraceToken(Token(SyntaxKind.OpenBraceToken))
            .WithCloseBraceToken(Token(SyntaxKind.CloseBraceToken))
            .AddModifiers(
                Token(SyntaxKind.PublicKeyword),
                Token(SyntaxKind.SealedKeyword),
                Token(SyntaxKind.PartialKeyword)
            );

        if(r.EsexprOptions.TryGetValue(out var esexprOptions)) {
            rec = rec.AddAttributeLists(AttributeList(SeparatedList<AttributeSyntax>(new AttributeSyntax[] {
                CodecAttr,
                ConstructorAttr(esexprOptions.Constructor),
            })));
        }

        if(dfn.TypeParameters.Count > 0) {
            rec = rec.AddTypeParameterListParameters(EmitTypeParameters(dfn.TypeParameters));
        }
        
        rec = rec.AddMembers(EmitRecordFields(r.Fields));

        return rec;
    }

    private MemberDeclarationSyntax EmitEnumDefinition(DefinitionInfo dfn, EnumDefinition e) {
        var recName = ConvertIdPascal(dfn.Name.Name);
        var rec = RecordDeclaration(Token(SyntaxKind.RecordKeyword), recName)
            .WithOpenBraceToken(Token(SyntaxKind.OpenBraceToken))
            .WithCloseBraceToken(Token(SyntaxKind.CloseBraceToken))
            .AddModifiers(
                Token(SyntaxKind.PublicKeyword),
                Token(SyntaxKind.AbstractKeyword),
                Token(SyntaxKind.PartialKeyword)
            );

        if(e.EsexprOptions.IsSome) {
            rec = rec.AddAttributeLists(AttributeList(SeparatedList<AttributeSyntax>(new AttributeSyntax[] {
                CodecAttr,
            })));
        }

        if(dfn.TypeParameters.Count > 0) {
            rec = rec.AddTypeParameterListParameters(EmitTypeParameters(dfn.TypeParameters));
        }

        TypeSyntax baseType;
        if(dfn.TypeParameters.Count > 0) {
            baseType = GenericName(recName)
                .WithTypeArgumentList(TypeArgumentList(SeparatedList<TypeSyntax>(
                    dfn.TypeParameters.Select(tp => tp switch {
                        TypeParameter.Type t => IdentifierName(ConvertIdPascal(t.Name)),
                        _ => throw new InvalidOperationException(),
                    }).ToArray()
                )));
        }
        else {
            baseType = IdentifierName(recName);
        }

        rec = rec.AddMembers(
            ConstructorDeclaration(recName)
                .WithBody(Block())
                .AddModifiers(Token(SyntaxKind.PrivateKeyword))
        );

        foreach(var c in e.Cases) {
            var caseRec = RecordDeclaration(Token(SyntaxKind.RecordKeyword), ConvertIdPascal(c.Name))
                .WithBaseList(BaseList(SeparatedList(new BaseTypeSyntax[] {
                    SimpleBaseType(baseType),
                })))
                .WithOpenBraceToken(Token(SyntaxKind.OpenBraceToken))
                .WithCloseBraceToken(Token(SyntaxKind.CloseBraceToken))
                .AddModifiers(
                    Token(SyntaxKind.PublicKeyword),
                    Token(SyntaxKind.SealedKeyword)
                );
            
            if(c.EsexprOptions.TryGetValue(out var esexprOptions)) {
                switch(esexprOptions.CaseType) {
                    case EsexprEnumCaseType.Constructor constructor:
                        caseRec = caseRec.AddAttributeLists(AttributeList(SeparatedList<AttributeSyntax>(new AttributeSyntax[] {
                            ConstructorAttr(constructor.Name),
                        })));
                        break;
                    
                    case EsexprEnumCaseType.InlineValue:
                        caseRec = caseRec.AddAttributeLists(AttributeList(SeparatedList<AttributeSyntax>(new AttributeSyntax[] {
                            InlineValueAttr,
                        })));
                        break;
                    
                    default:
                        throw new InvalidOperationException();
                }
            }
            
            caseRec = caseRec.AddMembers(EmitRecordFields(c.Fields));
            
            rec = rec.AddMembers(caseRec);
        }

        return rec;
    }

    private MemberDeclarationSyntax EmitSimpleDefinition(DefinitionInfo dfn, SimpleEnumDefinition e) {
        var enumType = EnumDeclaration(ConvertIdPascal(dfn.Name.Name))
            .AddModifiers(
                Token(SyntaxKind.PublicKeyword)
            );
        
        foreach(var c in e.Cases) {
            var member = EnumMemberDeclaration(ConvertIdPascal(c.Name));
            
            
            if(c.EsexprOptions.TryGetValue(out var esexprOptions)) {
                member = member.AddAttributeLists(AttributeList(SeparatedList<AttributeSyntax>(new AttributeSyntax[] {
                    ConstructorAttr(esexprOptions.Name),
                })));
            }
            
            
            enumType = enumType.AddMembers(member);
        }

        return enumType;
    }

    private MemberDeclarationSyntax EmitInterfaceDefinition(DefinitionInfo dfn, InterfaceDefinition iface) {
        var ifaceDecl = InterfaceDeclaration(ConvertIdPascal(dfn.Name.Name));

        if(dfn.TypeParameters.Count > 0) {
            ifaceDecl = ifaceDecl.AddTypeParameterListParameters(EmitTypeParameters(dfn.TypeParameters));
        }

        foreach(var method in iface.Methods) {
            var returnType = ValueTaskType(EmitReturnType(method.ReturnType));
            var methodDecl = MethodDeclaration(returnType, ConvertIdPascal(method.Name));

            if(method.TypeParameters.Count > 0) {
                methodDecl = methodDecl.AddTypeParameterListParameters(EmitTypeParameters(method.TypeParameters));
            }

            methodDecl = methodDecl.AddParameterListParameters(
                method.Parameters
                    .Select(p =>
                        Parameter(Identifier(ConvertIdCamel(p.Name)))
                            .WithType(EmitType(p.ParameterType))
                    )
                    .ToArray()
            );

            methodDecl = methodDecl.WithSemicolonToken(Token(SyntaxKind.SemicolonToken));

            ifaceDecl = ifaceDecl.AddMembers(methodDecl);
        }
        
        return ifaceDecl;
    }

    private MemberDeclarationSyntax EmitExceptionTypeDefinition(DefinitionInfo dfn, ExceptionTypeDefinition ex) {
        var className = ConvertIdPascal(dfn.Name.Name);
        TypeSyntax informationType = EmitType(ex.Information);
        var exceptionType = ParseTypeName("global::System.Exception");

        var informationProperty = PropertyDeclaration(informationType, "Information")
            .AddModifiers(Token(SyntaxKind.PublicKeyword))
            .WithAccessorList(
                AccessorList(List(new[] {
                    AccessorDeclaration(SyntaxKind.GetAccessorDeclaration).WithSemicolonToken(Token(SyntaxKind.SemicolonToken)),
                }))
            );

        var constructor1 = ConstructorDeclaration(className)
            .AddModifiers(Token(SyntaxKind.PublicKeyword))
            .AddParameterListParameters(
                Parameter(Identifier("information")).WithType(informationType)
            )
            .WithInitializer(ConstructorInitializer(SyntaxKind.BaseConstructorInitializer))
            .WithBody(Block(
                ExpressionStatement(AssignmentExpression(
                    SyntaxKind.SimpleAssignmentExpression,
                    IdentifierName("Information"),
                    IdentifierName("information")
                ))
            ));

        var constructor2 = ConstructorDeclaration(className)
            .AddModifiers(Token(SyntaxKind.PublicKeyword))
            .AddParameterListParameters(
                Parameter(Identifier("information")).WithType(informationType),
                Parameter(Identifier("message")).WithType(PredefinedType(Token(SyntaxKind.StringKeyword)))
            )
            .WithInitializer(ConstructorInitializer(SyntaxKind.BaseConstructorInitializer)
                .AddArgumentListArguments(Argument(IdentifierName("message"))))
            .WithBody(Block(
                ExpressionStatement(AssignmentExpression(
                    SyntaxKind.SimpleAssignmentExpression,
                    IdentifierName("Information"),
                    IdentifierName("information")
                ))
            ));

        var constructor3 = ConstructorDeclaration(className)
            .AddModifiers(Token(SyntaxKind.PublicKeyword))
            .AddParameterListParameters(
                Parameter(Identifier("information")).WithType(informationType),
                Parameter(Identifier("message")).WithType(PredefinedType(Token(SyntaxKind.StringKeyword))),
                Parameter(Identifier("innerException")).WithType(exceptionType)
            )
            .WithInitializer(ConstructorInitializer(SyntaxKind.BaseConstructorInitializer)
                .AddArgumentListArguments(
                    Argument(IdentifierName("message")),
                    Argument(IdentifierName("innerException"))
                ))
            .WithBody(Block(
                ExpressionStatement(AssignmentExpression(
                    SyntaxKind.SimpleAssignmentExpression,
                    IdentifierName("Information"),
                    IdentifierName("information")
                ))
            ));

        return ClassDeclaration(className)
            .AddModifiers(Token(SyntaxKind.PublicKeyword))
            .AddBaseListTypes(SimpleBaseType(exceptionType)) // Inherit from System.Exception
            .AddMembers(informationProperty, constructor1, constructor2, constructor3);

    }


    private TypeParameterSyntax[] EmitTypeParameters(IEnumerable<TypeParameter> typeParameters) =>
        typeParameters
            .Select(tp => tp switch {
                TypeParameter.Type t => TypeParameter(ConvertIdPascal(t.Name)),
                _ => throw new InvalidOperationException(),
            })
            .ToArray();

    private MemberDeclarationSyntax[] EmitRecordFields(IEnumerable<RecordField> fields) =>
        fields.Select(field => {
            var prop = PropertyDeclaration(EmitType(field.FieldType), ConvertIdPascal(field.Name))
                .WithAccessorList(AccessorList(List(new AccessorDeclarationSyntax[] {
                    AccessorDeclaration(SyntaxKind.GetAccessorDeclaration).WithSemicolonToken(Token(SyntaxKind.SemicolonToken)),
                    AccessorDeclaration(SyntaxKind.InitAccessorDeclaration).WithSemicolonToken(Token(SyntaxKind.SemicolonToken)),
                })))
                .AddModifiers(
                    Token(SyntaxKind.PublicKeyword),
                    Token(SyntaxKind.RequiredKeyword)
                );

            if(field.EsexprOptions.TryGetValue(out var esexprOptions)) {
                switch(esexprOptions.Kind) {
                    case EsexprRecordFieldKind.Positional positional:
                        switch(positional.Mode) {
                            case EsexprRecordPositionalMode.Required:
                                break;
                                
                            case EsexprRecordPositionalMode.Optional:
                                prop = prop.AddAttributeLists(AttributeList(SingletonSeparatedList(
                                    OptionalAttr
                                )));
                                break;
                            
                            default: throw new InvalidOperationException();
                        }
                        break;
                    
                    case EsexprRecordFieldKind.Keyword keyword:
                        prop = prop.AddAttributeLists(AttributeList(SingletonSeparatedList(
                            KeywordAttr(keyword.Name)
                        )));
                        
                        switch(keyword.Mode) {
                            case EsexprRecordKeywordMode.Required:
                                break;
                                
                            case EsexprRecordKeywordMode.Optional:
                                prop = prop.AddAttributeLists(AttributeList(SingletonSeparatedList(
                                    OptionalAttr
                                )));
                                break;

                            case EsexprRecordKeywordMode.DefaultValue defaultValue:
                            {
                                var defaultValueExpr = GetValueExpr(defaultValue.Value);
                                prop = prop.AddAttributeLists(AttributeList(SingletonSeparatedList(
                                    DefaultValueAttr(defaultValueExpr.NormalizeWhitespace().ToString())
                                )));
                                break;
                            }
                            
                            default: throw new InvalidOperationException();
                        }
                        break;
                    
                    case EsexprRecordFieldKind.Dict:
                        prop = prop.AddAttributeLists(AttributeList(SingletonSeparatedList(
                            DictAttr
                        )));
                        break;
                    
                    case EsexprRecordFieldKind.Vararg:
                        prop = prop.AddAttributeLists(AttributeList(SingletonSeparatedList(
                            VarargAttr
                        )));
                        break;
                    
                    default: throw new InvalidOperationException();
                }
            }
            
            return prop;
        }).ToArray<MemberDeclarationSyntax>();

    private ExpressionSyntax GetValueExpr(EsexprDecodedValue value) {
        switch(value) {
            case EsexprDecodedValue.Record record:
            {
                var fieldInits = record.Fields
                    .Select(f =>
                        AssignmentExpression(
                            SyntaxKind.SimpleAssignmentExpression,
                            IdentifierName(ConvertIdPascal(f.Name)),
                            GetValueExpr(f.Value)
                        )
                    )
                    .ToArray<ExpressionSyntax>();

                return ObjectCreationExpression(EmitType(record.T))
                    .WithInitializer(
                        InitializerExpression(SyntaxKind.ObjectInitializerExpression)
                            .AddExpressions(fieldInits)
                    );
            }

            case EsexprDecodedValue.Enum enumValue:
            {
                var fieldInits = enumValue.Fields
                    .Select(f =>
                        AssignmentExpression(
                            SyntaxKind.SimpleAssignmentExpression,
                            IdentifierName(ConvertIdPascal(f.Name)),
                            GetValueExpr(f.Value)
                        )
                    )
                    .ToArray<ExpressionSyntax>();

                TypeSyntax caseType = QualifiedName(
                    (NameSyntax)EmitType(enumValue.T),
                    IdentifierName(ConvertIdPascal(enumValue.CaseName))
                );

                return ObjectCreationExpression(caseType)
                    .WithInitializer(
                        InitializerExpression(SyntaxKind.ObjectInitializerExpression)
                            .AddExpressions(fieldInits)
                    );
            }

            case EsexprDecodedValue.Optional optional:
            {
                if(optional.Value.TryGetValue(out var elementValue)) {
                    return InvocationExpression(
                            QualifiedName(
                                (NameSyntax)EmitTypeUnmapped(optional.T),
                                IdentifierName("Some")
                            )
                    )
                        .AddArgumentListArguments(Argument(GetValueExpr(elementValue)));
                }
                else {
                    return QualifiedName(
                        (NameSyntax)EmitTypeUnmapped(optional.T),
                        IdentifierName("None")
                    );
                }
            }

            case EsexprDecodedValue.Vararg vararg:
                return InvocationExpression(
                        QualifiedName(
                            (NameSyntax)EmitTypeUnmapped(vararg.T),
                            IdentifierName("FromValues")
                        )
                    )
                    .AddArgumentListArguments(Argument(
                        CollectionExpression(SeparatedList(
                            vararg.Values
                                .Select(v => ExpressionElement(GetValueExpr(v)))
                                .ToArray<CollectionElementSyntax>()
                        ))
                    ));

            case EsexprDecodedValue.Dict dict:
                return InvocationExpression(
                        QualifiedName(
                            (NameSyntax)EmitTypeUnmapped(dict.T),
                            IdentifierName("FromValues")
                        )
                    )
                    .AddArgumentListArguments(Argument(
                        ObjectCreationExpression(
                            QualifiedName(
                                QualifiedName(
                                    QualifiedName(
                                        AliasQualifiedName(
                                            IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                                            IdentifierName("System")
                                        ),
                                        IdentifierName("Collections")
                                    ),
                                    IdentifierName("Generic")
                                ),
                                GenericName("Dictionary")
                                    .AddTypeArgumentListArguments(
                                        PredefinedType(Token(SyntaxKind.StringKeyword)),
                                        EmitType(dict.ElementType)
                                    )
                            )
                        )
                            .WithInitializer(
                                InitializerExpression(SyntaxKind.CollectionInitializerExpression)
                                    .AddExpressions(
                                        dict.Values
                                            .Select(v =>
                                                InitializerExpression(SyntaxKind.ComplexElementInitializerExpression)
                                                    .AddExpressions(
                                                        LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(v.Key)),
                                                        GetValueExpr(v.Value)
                                                    )
                                            )
                                            .ToArray<ExpressionSyntax>()
                                    )
                            )
                    ));
            
            case EsexprDecodedValue.BuildFrom buildFrom:
                return InvocationExpression(
                        QualifiedName(
                            (NameSyntax)EmitTypeUnmapped(buildFrom.T),
                            IdentifierName("BuildFrom")
                        )
                    )
                    .AddArgumentListArguments(Argument(
                        GetValueExpr(buildFrom.FromValue)
                    ));
                
            case EsexprDecodedValue.FromBool fromBool:
                return InvocationExpression(
                        QualifiedName(
                            (NameSyntax)EmitTypeUnmapped(fromBool.T),
                            IdentifierName("FromBool")
                        )
                    )
                    .AddArgumentListArguments(Argument(
                        LiteralExpression(fromBool.B
                            ? SyntaxKind.TrueLiteralExpression
                            : SyntaxKind.FalseLiteralExpression)
                    ));

            case EsexprDecodedValue.FromInt fromInt:
            {
                string? methodName = null;
                ExpressionSyntax? valueExpr = null;

                {
                    if(fromInt.MinInt.TryGetValue(out var min) && fromInt.MaxInt.TryGetValue(out var max)) {
                        if(min >= 0) {
                            if(max <= byte.MaxValue) {
                                methodName = "fromByte";
                                valueExpr = LiteralExpression(
                                    SyntaxKind.NumericLiteralExpression,
                                    Literal((byte)fromInt.I)
                                );
                            }
                            else if(max <= ushort.MaxValue) {
                                methodName = "fromUInt16";
                                valueExpr = LiteralExpression(
                                    SyntaxKind.NumericLiteralExpression,
                                    Literal((ushort)fromInt.I)
                                );
                            }
                            else if(max <= uint.MaxValue) {
                                methodName = "fromUInt32";
                                valueExpr = LiteralExpression(
                                    SyntaxKind.NumericLiteralExpression,
                                    Literal((uint)fromInt.I)
                                );
                            }
                            else if(max <= ulong.MaxValue) {
                                methodName = "fromUInt64";
                                valueExpr = LiteralExpression(
                                    SyntaxKind.NumericLiteralExpression,
                                    Literal((ulong)fromInt.I)
                                );
                            }
                            else if(max <= UInt128.MaxValue) {
                                methodName = "fromUInt128";
                                var value128 = (UInt128)fromInt.I;
                                valueExpr = ObjectCreationExpression(
                                    QualifiedName(
                                        AliasQualifiedName(
                                            IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                                            IdentifierName("System")
                                        ),
                                        IdentifierName("UInt128")
                                    )
                                ).AddArgumentListArguments(
                                    Argument(LiteralExpression(
                                        SyntaxKind.NumericLiteralExpression,
                                        Literal(unchecked((ulong)(value128 >> 64)))
                                    )),
                                    Argument(LiteralExpression(
                                        SyntaxKind.NumericLiteralExpression,
                                        Literal(unchecked((ulong)value128))
                                    ))
                                );
                            }
                        }
                        else {
                            if(min >= sbyte.MinValue && max <= sbyte.MaxValue) {
                                methodName = "fromSByte";
                                valueExpr = LiteralExpression(
                                    SyntaxKind.NumericLiteralExpression,
                                    Literal((sbyte)fromInt.I)
                                );
                            }
                            else if(min >= short.MinValue && max <= short.MaxValue) {
                                methodName = "fromInt16";
                                valueExpr = LiteralExpression(
                                    SyntaxKind.NumericLiteralExpression,
                                    Literal((short)fromInt.I)
                                );
                            }
                            else if(min >= int.MinValue && max <= int.MaxValue) {
                                methodName = "fromInt32";
                                valueExpr = LiteralExpression(
                                    SyntaxKind.NumericLiteralExpression,
                                    Literal((int)fromInt.I)
                                );
                            }
                            else if(min >= long.MinValue && max <= long.MaxValue) {
                                methodName = "fromInt64";
                                valueExpr = LiteralExpression(
                                    SyntaxKind.NumericLiteralExpression,
                                    Literal((long)fromInt.I)
                                );
                            }
                            else if(min >= Int128.MinValue && max <= Int128.MaxValue) {
                                methodName = "fromInt128";
                                var value128 = (Int128)fromInt.I;
                                valueExpr = ObjectCreationExpression(
                                    QualifiedName(
                                        AliasQualifiedName(
                                            IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                                            IdentifierName("System")
                                        ),
                                        IdentifierName("UInt128")
                                    )
                                ).AddArgumentListArguments(
                                    Argument(LiteralExpression(
                                        SyntaxKind.NumericLiteralExpression,
                                        Literal(unchecked((ulong)((UInt128)value128 >> 64)))
                                    )),
                                    Argument(LiteralExpression(
                                        SyntaxKind.NumericLiteralExpression,
                                        Literal(unchecked((ulong)value128))
                                    ))
                                );
                            }
                        }
                    }
                }

                if(methodName is null || valueExpr is null) {
                    valueExpr = InvocationExpression(
                        QualifiedName(
                            QualifiedName(
                                QualifiedName(
                                    AliasQualifiedName(
                                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                                        IdentifierName("System")
                                    ),
                                    IdentifierName("Numerics")
                                ),
                                IdentifierName("BigInteger")
                            ),
                            IdentifierName("Parse")
                        )
                    ).AddArgumentListArguments(
                        Argument(
                            LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(fromInt.I.ToString()))
                        )
                    );
                    
                    if(fromInt.MinInt.TryGetValue(out var min) && min >= 0) {
                        methodName = "FromNat";
                        valueExpr = ObjectCreationExpression(
                            QualifiedName(
                                QualifiedName(
                                    AliasQualifiedName(
                                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                                        IdentifierName("NobleIDL")
                                    ),
                                    IdentifierName("Runtime")
                                ),
                                IdentifierName("Nat")
                            )
                        ).AddArgumentListArguments(
                            Argument(valueExpr)
                        );
                    }
                    else {
                        methodName = "FromBigInteger";
                    }
                }

                return InvocationExpression(
                        QualifiedName(
                            (NameSyntax)EmitTypeUnmapped(fromInt.T),
                            IdentifierName(methodName)
                        )
                    )
                    .AddArgumentListArguments(Argument(valueExpr));
                
            }
                
            case EsexprDecodedValue.FromStr fromStr:
                return InvocationExpression(
                        QualifiedName(
                            (NameSyntax)EmitType(fromStr.T),
                            IdentifierName("FromString")
                        )
                    )
                    .AddArgumentListArguments(Argument(
                        LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(fromStr.S))
                    ));
                
            case EsexprDecodedValue.FromBinary fromBinary:

                static ExpressionSyntax CreateByteArrayExpression(byte[] byteArray) {
                    var byteLiterals = byteArray
                        .Select(b =>
                            LiteralExpression(
                                SyntaxKind.NumericLiteralExpression,
                                Literal(b)
                            )
                        )
                        .ToArray<ExpressionSyntax>();

                    var arrayInitializer = InitializerExpression(
                        SyntaxKind.ArrayInitializerExpression,
                        SeparatedList(byteLiterals)
                    );

                    var arrayCreation = ArrayCreationExpression(
                        ArrayType(
                            PredefinedType(Token(SyntaxKind.ByteKeyword))
                        ).WithRankSpecifiers(
                            SingletonList(
                                ArrayRankSpecifier(
                                    SingletonSeparatedList<ExpressionSyntax>(
                                        OmittedArraySizeExpression()
                                    )
                                )
                            )
                        )
                    ).WithInitializer(arrayInitializer);

                    return arrayCreation;
                }

                return InvocationExpression(
                        QualifiedName(
                            (NameSyntax)EmitType(fromBinary.T),
                            IdentifierName("FromByteArray")
                        )
                    )
                    .AddArgumentListArguments(Argument(
                        CreateByteArrayExpression(fromBinary.B)
                    ));
                
                
            case EsexprDecodedValue.FromFloat32 fromFloat32:
                return InvocationExpression(
                        QualifiedName(
                            (NameSyntax)EmitType(fromFloat32.T),
                            IdentifierName("FromSingle")
                        )
                    )
                    .AddArgumentListArguments(Argument(
                        LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(fromFloat32.F))
                    ));
                
            case EsexprDecodedValue.FromFloat64 fromFloat64:
                return InvocationExpression(
                        QualifiedName(
                            (NameSyntax)EmitType(fromFloat64.T),
                            IdentifierName("FromDouble")
                        )
                    )
                    .AddArgumentListArguments(Argument(
                        LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(fromFloat64.F))
                    ));
                
            case EsexprDecodedValue.FromNull fromNull:
                return InvocationExpression(
                    QualifiedName(
                        (NameSyntax)EmitType(fromNull.T),
                        IdentifierName("FromNull")
                    )
                );
            
            default: throw new InvalidOperationException();
        };
    }

    private TypeSyntax EmitType(TypeExpr t) {
        var (res, _) = EmitTypeCommon(t, ignoreMapping: false);
        return res;
    }
    
    private TypeSyntax EmitTypeUnmapped(TypeExpr t) {
        var (res, _) = EmitTypeCommon(t, ignoreMapping: true);
        return res;
    }

    private TypeSyntax? EmitReturnType(TypeExpr t) {
        var (res, isVoid) = EmitTypeCommon(t, ignoreMapping: false);
        if(isVoid) {
            return null;
        }

        return res;
    }
    
    private (TypeSyntax, bool isVoid) EmitTypeCommon(TypeExpr t, bool ignoreMapping) {
        switch(t) {
            case TypeExpr.DefinedType { Name: var name } definedType
                when !ignoreMapping && GetMappedType(name) is {} mappedType:
            {
                var typeParamMap = GetTypeParameterMapping(definedType);
                var typeSyntax = MappedTypeToSyntax(mappedType, typeParamMap);
                var isVoid = mappedType is CsharpMappedType.Void;
                return (typeSyntax, isVoid);
            }
            
            case TypeExpr.DefinedType { Name: var name, Args: var args }:
            {
                SimpleNameSyntax memberName;
                if(args.Count > 0) {
                    memberName = GenericName(ConvertIdPascal(name.Name))
                        .AddTypeArgumentListArguments(args.Select(EmitType).ToArray());
                }
                else {
                    memberName = IdentifierName(ConvertIdPascal(name.Name));
                }

                var typeSyntax = GetNamespaceMember(name.Package, memberName);
                return (typeSyntax, false);
            }
            
            case TypeExpr.TypeParameter tp:
                return (IdentifierName(ConvertIdPascal(tp.Name)), false);
            
            default:
                throw new InvalidOperationException();
        }
    }

    private IDictionary<string, TypeExpr> GetTypeParameterMapping(TypeExpr.DefinedType t) {
        var dfn = Model.Definitions.First(d => d.Name == t.Name);

        var typeParameters = dfn.TypeParameters;

        if(typeParameters.Count != t.Args.Count) {
            throw new NobleIDLCompileErrorException("Type parameter mismatch");
        }

        return typeParameters.Zip(t.Args).ToDictionary(
            pair => pair.First switch {
                TypeParameter.Type t => t.Name,
                _ => throw new InvalidOperationException(),
            },
            pair => pair.Second
        );
    }
    
    private CsharpMappedType? GetMappedType(QualifiedName name) {
        var dfn = Model.Definitions.First(d => d.Name == name);
        if(dfn.Definition is not Definition.ExternType) {
            return null;
        }

        IESExprCodec<CsharpAnnExternType> codec = new CsharpAnnExternType.Codec();
        
        return dfn.Annotations
            .Where(a => a.Scope == "csharp")
            .Select(a => codec.Decode(a.Value))
            .OfType<CsharpAnnExternType.MappedTo>()
            .Select(mapped => mapped.CsharpType)
            .FirstOrDefault();
    }

    private TypeSyntax MappedTypeToSyntax(CsharpMappedType mappedType, IDictionary<string, TypeExpr> typeParameters) {
        SimpleNameSyntax GetName(string name, IReadOnlyList<CsharpMappedType> args) {
            if(args.Count > 0) {
                return GenericName(name)
                    .AddTypeArgumentListArguments(
                        args
                            .Select(arg => MappedTypeToSyntax(arg, typeParameters))
                            .ToArray()
                    );
            }
            else {
                return IdentifierName(name);
            }
        }
        
        switch(mappedType) {
            case CsharpMappedType.Global { Name: var name, Args: var args }:
                return AliasQualifiedName(
                    IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                    GetName(name, args)
                );

            case CsharpMappedType.Member { Parent: var parent, Name: var name, Args: var args }:
            {
                var parentType = MappedTypeToSyntax(parent, typeParameters);
                if(parentType is not NameSyntax parentTypeName) {
                    throw new NobleIDLCompileErrorException("Invalid parent type");
                }
                
                return QualifiedName(parentTypeName, GetName(name, args));
            }

            case CsharpMappedType.TypeParameter { Name: var name }:
            {
                if(!typeParameters.TryGetValue(name, out var t)) {
                    throw new NobleIDLCompileErrorException("Invalid type parameter");
                }

                return EmitType(t);
            }
            
            case CsharpMappedType.Array { ElementType: var elementType }:
                return ArrayType(MappedTypeToSyntax(elementType, typeParameters));
            
            case CsharpMappedType.Pointer { PointedToType: var pointedToType }:
                return PointerType(MappedTypeToSyntax(pointedToType, typeParameters));
            
            
            case CsharpMappedType.Void:
                return ParseTypeName("System.ValueTuple");
            
            default:
                throw new InvalidOperationException();
        }
    }

    private AttributeSyntax CodecAttr =>
        Attribute(
            QualifiedName(
                QualifiedName(
                    AliasQualifiedName(
                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                        IdentifierName("ESExpr")
                    ),
                    IdentifierName("Runtime")
                ),
                IdentifierName("ESExprCodec")
            )
        );

    private AttributeSyntax ConstructorAttr(string name) =>
        Attribute(
            QualifiedName(
                QualifiedName(
                    AliasQualifiedName(
                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                        IdentifierName("ESExpr")
                    ),
                    IdentifierName("Runtime")
                ),
                IdentifierName("Constructor")
            )
        )
            .WithArgumentList(AttributeArgumentList(SeparatedList(new AttributeArgumentSyntax[] {
                AttributeArgument(LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(name))),
            })));

    private AttributeSyntax InlineValueAttr =>
        Attribute(
            QualifiedName(
                QualifiedName(
                    AliasQualifiedName(
                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                        IdentifierName("ESExpr")
                    ),
                    IdentifierName("Runtime")
                ),
                IdentifierName("InlineValue")
            )
        );

    private AttributeSyntax KeywordAttr(string keyword) =>
        Attribute(
            QualifiedName(
                QualifiedName(
                    AliasQualifiedName(
                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                        IdentifierName("ESExpr")
                    ),
                    IdentifierName("Runtime")
                ),
                IdentifierName("Keyword")
            )
        )
            .WithArgumentList(AttributeArgumentList(SeparatedList(new AttributeArgumentSyntax[] {
                AttributeArgument(LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(keyword))),
            })));

    private AttributeSyntax OptionalAttr =>
        Attribute(
            QualifiedName(
                QualifiedName(
                    AliasQualifiedName(
                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                        IdentifierName("ESExpr")
                    ),
                    IdentifierName("Runtime")
                ),
                IdentifierName("Optional")
            )
        );

    private AttributeSyntax DefaultValueAttr(string defaultValue) =>
        Attribute(
            QualifiedName(
                QualifiedName(
                    AliasQualifiedName(
                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                        IdentifierName("ESExpr")
                    ),
                    IdentifierName("Runtime")
                ),
                IdentifierName("DefaultValue")
            )
        )
            .WithArgumentList(AttributeArgumentList(SeparatedList(new AttributeArgumentSyntax[] {
                AttributeArgument(LiteralExpression(SyntaxKind.StringLiteralExpression, Literal(defaultValue))),
            })));

    private AttributeSyntax DictAttr =>
        Attribute(
            QualifiedName(
                QualifiedName(
                    AliasQualifiedName(
                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                        IdentifierName("ESExpr")
                    ),
                    IdentifierName("Runtime")
                ),
                IdentifierName("Dict")
            )
        );

    private AttributeSyntax VarargAttr =>
        Attribute(
            QualifiedName(
                QualifiedName(
                    AliasQualifiedName(
                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                        IdentifierName("ESExpr")
                    ),
                    IdentifierName("Runtime")
                ),
                IdentifierName("Vararg")
            )
        );


    private TypeSyntax ValueTaskType(TypeSyntax? t) =>
        QualifiedName(
            QualifiedName(
                QualifiedName(
                    AliasQualifiedName(
                        IdentifierName(Token(SyntaxKind.GlobalKeyword)),
                        IdentifierName("System")
                    ),
                    IdentifierName("Threading")
                ),
                IdentifierName("Tasks")
            ),
            t is null
                ? IdentifierName("ValueTask")
                : GenericName(Identifier("ValueTask")).AddTypeArgumentListArguments(t)
        );

    private string ConvertIdPascal(string kebab) =>
        string.Concat(
            kebab.Split("-").Select(s => s.Substring(0, 1).ToUpperInvariant() + s.Substring(1))
        );

    private string ConvertIdCamel(string kebab) {
        var idStr = string.Concat(
            kebab.Split("-").Select((s, i) => i == 0 ? s : s.Substring(0, 1).ToUpperInvariant() + s.Substring(1))
        );

        if(SyntaxFacts.GetKeywordKind(idStr) != SyntaxKind.None) {
            idStr = "@" + idStr;
        }

        return idStr;
    }
        
}