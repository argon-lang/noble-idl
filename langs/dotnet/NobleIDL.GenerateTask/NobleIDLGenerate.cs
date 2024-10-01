using System.Collections.Immutable;
using System.Text;
using Microsoft.Build.Framework;
using NobleIDL.Backend;
using Task = Microsoft.Build.Utilities.Task;

namespace NobleIDL.GenerateTask;

public sealed class NobleIDLGenerate : Task {
    
    [Required]
    public required ITaskItem OutputFile { get; set; }
    
    [Required]
    public required ITaskItem[] InputFiles { get; set; }
    
    [Required]
    public required ITaskItem[] Dependencies { get; set; }

    public ITaskItem[] NobleIDLPackages { get; set; } = [];
    
    
    public override bool Execute() {
        var nsMapping = GetNamespaceMapping();
        
        var inputFileData = InputFiles
            .Select(item => File.ReadAllText(item.ItemSpec, Encoding.UTF8))
            .ToImmutableList();

        if(Log.HasLoggedErrors) {
            return false;
        }

        try {
            var dir = Path.GetDirectoryName(OutputFile.ItemSpec);
            if(dir is not null) {
                Directory.CreateDirectory(dir);   
            }
            
            CSharpNobleIDLCompiler.Compile(
                new CSharpIDLCompilerOptions {
                    LanguageOptions = new CSharpLanguageOptions {
                        NamespaceMapping = nsMapping,
                        OutputFile = OutputFile.ItemSpec,
                    },
                    InputFileData = inputFileData,
                    LibraryFileData = [],
                },
                Dependencies.Select(item => item.ItemSpec)
            ).AsTask().Wait();
        }
        catch(Exception ex) {
            Log.LogErrorFromException(ex);
        }

        return !Log.HasLoggedErrors;
    }

    private NamespaceMapping GetNamespaceMapping() {
        var mapping = new Dictionary<string, string>();
        foreach(var item in NobleIDLPackages) {
            var packageName = item.ItemSpec;
            var ns = item.GetMetadata("Namespace");
            if(ns is null) {
                Log.LogError($"NobleIDL package '{packageName}' is missing Namespace");
                continue;
            }
            
            mapping[packageName] = ns;
        }

        return new NamespaceMapping {
            Mapping = mapping.ToImmutableDictionary(),
        };
    }
}