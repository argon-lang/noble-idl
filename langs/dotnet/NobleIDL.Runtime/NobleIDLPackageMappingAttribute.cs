namespace NobleIDL.Runtime;

[AttributeUsage(AttributeTargets.Assembly, AllowMultiple = true)]
public class NobleIDLPackageMappingAttribute : Attribute {

    public NobleIDLPackageMappingAttribute(string packageName, string namespaceName) {
        PackageName = packageName;
        NamespaceName = namespaceName;
    }

    public string PackageName { get; }
    public string NamespaceName { get; }
}