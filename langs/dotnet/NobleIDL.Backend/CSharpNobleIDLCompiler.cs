using System.Collections.Immutable;
using System.Reflection.Metadata;
using System.Reflection.PortableExecutable;
using NobleIDL.Backend.Api;

namespace NobleIDL.Backend;

public static class CSharpNobleIDLCompiler {
    
    public static async ValueTask<NobleIdlGenerationResult> Compile(
        CSharpIDLCompilerOptions options,
        CancellationToken cancellationToken = default)
    {
        var modelOptions = new NobleIdlCompileModelOptions {
            Files = options.InputFileData,
            LibraryFiles = options.LibraryFileData,
        };
        
        NobleIdlCompileModelResult result;
        using(var compiler = new NobleIDLCompiler()) {
            result = compiler.LoadModel(modelOptions);
        }

        if(result is not NobleIdlCompileModelResult.Success { Model: var model }) {
            if(result is NobleIdlCompileModelResult.Failure failure) {
                throw new NobleIDLCompileErrorException(string.Join("\n", failure.Errors));
            }
            else {
                throw new Exception("Unexpected result");
            }
        }

        var backend = new CSharpBackend(
            new NobleIdlGenerationRequest<CSharpLanguageOptions> {
                Model = model,
                LanguageOptions = options.LanguageOptions,
            },
            options.InputFileData
        );
        await backend.Emit(cancellationToken);

        return backend.Result;
    }

    public static async ValueTask<NobleIdlGenerationResult> Compile(
        CSharpIDLCompilerOptions options,
        IEnumerable<string> assemblyPaths,
        CancellationToken cancellationToken = default
    ) {
        foreach(var asmPath in assemblyPaths) {
            options = await UpdateOptionsFrom(options, asmPath);   
        }
        
        return await Compile(options, cancellationToken);
    }

    private static async ValueTask<CSharpIDLCompilerOptions> UpdateOptionsFrom(
        CSharpIDLCompilerOptions options,
        string assemblyPath
    ) {
        await using var stream = File.OpenRead(assemblyPath);
        using var peReader = new PEReader(stream);
        if(!peReader.HasMetadata) {
            return options;
        }

        var metadataReader = peReader.GetMetadataReader();

        foreach(var handle in metadataReader.CustomAttributes) {
            var attribute = metadataReader.GetCustomAttribute(handle);
            var constructorHandle = attribute.Constructor;
            string attributeName = GetAttributeTypeName(metadataReader, constructorHandle);

            switch(attributeName) {
                case "NobleIDL.Runtime.NobleIDLSourceFileAttribute":
                {
                    var reader = metadataReader.GetBlobReader(attribute.Value);
                    reader.ReadUInt16();
                    var sourceText = reader.ReadSerializedString();
                    if(sourceText is null) {
                        throw new NobleIDLCompileErrorException("Error loading NobleIDLSourceFileAttribute data");
                    }

                    options = options with {
                        LibraryFileData = [..options.LibraryFileData, sourceText],
                    };
                    break;
                }
                    
                case "NobleIDL.Runtime.NobleIDLPackageMappingAttribute":
                {
                    var reader = metadataReader.GetBlobReader(attribute.Value);
                    reader.ReadUInt16();
                    var idlPackage = reader.ReadSerializedString();
                    var mappedNamespace = reader.ReadSerializedString();
                    if(idlPackage is null || mappedNamespace is null) {
                        throw new NobleIDLCompileErrorException("Error loading NobleIDLPackageMappingAttribute data");
                    }

                    options = options with {
                        LanguageOptions = options.LanguageOptions with {
                            NamespaceMapping = new NamespaceMapping {
                                Mapping = options.LanguageOptions.NamespaceMapping.Mapping.ImmutableDictionary
                                    .Add(idlPackage, mappedNamespace)
                                    .ToImmutableDictionary(),
                            },
                        },
                    };
                    break;                    
                }
                    
            }
        }

        return options;
    }
    
    
    private static string GetAttributeTypeName(MetadataReader reader, EntityHandle constructorHandle) {
        switch(constructorHandle.Kind) {
            case HandleKind.MethodDefinition:
            {
                var methodDef = reader.GetMethodDefinition((MethodDefinitionHandle)constructorHandle);
                return GetFullTypeName(reader, methodDef.GetDeclaringType());
            }
            case HandleKind.MemberReference:
            {
                var memberRef = reader.GetMemberReference((MemberReferenceHandle)constructorHandle);
                switch(memberRef.Parent.Kind) {
                    case HandleKind.TypeDefinition:
                        return GetFullTypeName(reader, (TypeDefinitionHandle)memberRef.Parent);
                        
                    case HandleKind.TypeReference:
                        return GetFullTypeName(reader, (TypeReferenceHandle)memberRef.Parent);
                    
                    default:
                        throw new NotSupportedException("Unsupported type handle kind.");
                }
            }

            default:
                throw new NotSupportedException("Unsupported constructor handle kind.");
        }
    }

    private static string GetFullTypeName(MetadataReader reader, TypeDefinitionHandle typeHandle) {
        var type = reader.GetTypeDefinition(typeHandle);
        var namespaceHandle = type.Namespace;
        var name = reader.GetString(type.Name);
        var namespaceName = reader.GetString(namespaceHandle);
        return string.IsNullOrEmpty(namespaceName) ? name : $"{namespaceName}.{name}";
    }

    private static string GetFullTypeName(MetadataReader reader, TypeReferenceHandle typeHandle) {
        var type = reader.GetTypeReference(typeHandle);
        var namespaceHandle = type.Namespace;
        var name = reader.GetString(type.Name);
        var namespaceName = reader.GetString(namespaceHandle);
        return string.IsNullOrEmpty(namespaceName) ? name : $"{namespaceName}.{name}";
    }
}