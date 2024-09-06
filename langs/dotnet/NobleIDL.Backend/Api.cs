using System.Collections.Immutable;
using ESExpr.Runtime;

namespace NobleIDL.Backend.Api;

public sealed partial record PackageName : IComparable<PackageName> {
    public static PackageName FromString(string name) {
        if(name.Length == 0) {
            return new PackageName {
                Parts = [],
            };
        }
        else {
            return new PackageName {
                Parts = name.Split('.').ToImmutableList(),
            };
        }
    }

    public int CompareTo(PackageName? other) {
        if(other is null) {
            return 1;
        }
        
        foreach(var (a, b) in Parts.Zip(other.Parts)) {
            int partRes = string.Compare(a, b, StringComparison.InvariantCulture);
            if(partRes != 0) {
                return partRes;
            }
        }
        
        return Parts.Count.CompareTo(other.Parts.Count);
    }

    public override string ToString() {
        return string.Join(".", Parts);
    }
}


[ESExprCodec]
public abstract partial record CsharpAnnExternType {
    private CsharpAnnExternType() { }
    
    public sealed record MappedTo : CsharpAnnExternType {
        public required CsharpMappedType CsharpType { get; init; } 
    }
}


[ESExprCodec]
public abstract partial record CsharpMappedType {
    private CsharpMappedType() { }
    
    public sealed record Global : CsharpMappedType {
        public required string Name { get; init; }
        
        [Vararg]
        public required VList<CsharpMappedType> Args { get; init; }
    }
    
    public sealed record Member : CsharpMappedType {
        public required CsharpMappedType Parent { get; init; }
        public required string Name { get; init; }
        
        [Vararg]
        public required VList<CsharpMappedType> Args { get; init; }
    }
    
    public sealed record TypeParameter : CsharpMappedType {
        public required string Name { get; init; } 
    }
    
    public sealed record Array : CsharpMappedType {
        public required CsharpMappedType ElementType { get; init; } 
    }
    
    public sealed record Pointer : CsharpMappedType {
        public required CsharpMappedType PointedToType { get; init; } 
    }

    public sealed record Void : CsharpMappedType;
}
