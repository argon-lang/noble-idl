using System.Collections.Immutable;
using CommandLine;
using CommandLine.Text;

namespace NobleIDL.Backend;

public static class Program {
    public class CLIOptions {
        [Option('i', "input", Required = true, HelpText = "Input file")]
        public required IEnumerable<string> InputDirectories { get; set; }
        
        [Option('l', "library", Required = true, HelpText = "Library file")]
        public required IEnumerable<string> LibraryDirectories { get; set; }
        
        [Option('o', "output", Required = true, HelpText = "Output file")]
        public required string OutputFile { get; set; }
        
        [Option("namespace", Required = false, HelpText = "Namespace mapping")]
        public IEnumerable<string> NamespaceMapping { get; set; } = [];

        [Option("ref", Required = false, HelpText = "Referenced assembly")]
        public IEnumerable<string> ReferencedAssemblies { get; set; } = [];
    }

    private static async Task<int> Main(string[] args) {
        var optionsRes = new CommandLine.Parser(settings => {
            settings.GetoptMode = true;
        }).ParseArguments<CLIOptions>(args);

        if(optionsRes is not Parsed<CLIOptions> parsed) {
            Console.Error.WriteLine("Invalid command line arguments");
            var sb = SentenceBuilder.Create();
            foreach(var error in ((NotParsed<CLIOptions>)optionsRes).Errors) {
                Console.Error.WriteLine(sb.FormatError(error));
            }
            return 1;
        }
        
        var cancellationTokenSource = new CancellationTokenSource();
        Console.CancelKeyPress += (_, args) => {
            if(!cancellationTokenSource.IsCancellationRequested) {
                args.Cancel = true;
                cancellationTokenSource.Cancel();
            }
        };
        
        var options = parsed.Value;

        var inputFiles = new List<string>();
        foreach(var inputDir in options.InputDirectories) {
            cancellationTokenSource.Token.ThrowIfCancellationRequested();
            inputFiles.Add(await File.ReadAllTextAsync(inputDir, cancellationTokenSource.Token));
        }

        var libraryFiles = new List<string>();
        foreach(var libDir in options.LibraryDirectories) {
            cancellationTokenSource.Token.ThrowIfCancellationRequested();
            libraryFiles.Add(await File.ReadAllTextAsync(libDir, cancellationTokenSource.Token));
        }
        
        var namespaceMapping = options.NamespaceMapping
            .Select(mapping => {
                var parts = mapping.Split('=', 2);
                if(parts is [var key, var value]) {
                    return (key, value);
                }
                else {
                    throw new Exception("Invalid namespace mapping: " + mapping);
                }
            })
            .ToImmutableDictionary(
                pair => pair.key,
                pair => pair.value
            );

        var compilerOptions = new CSharpIDLCompilerOptions {
            LanguageOptions = new CSharpLanguageOptions {
                OutputFile = options.OutputFile,
                NamespaceMapping = new NamespaceMapping {
                    Mapping = namespaceMapping,
                },
            },
            
            InputFileData = inputFiles.ToImmutableList(),
            LibraryFileData = libraryFiles.ToImmutableList(),
        };
        
        await CSharpNobleIDLCompiler.Compile(compilerOptions, options.ReferencedAssemblies, cancellationTokenSource.Token);

        return 0;
    }
    
    
}