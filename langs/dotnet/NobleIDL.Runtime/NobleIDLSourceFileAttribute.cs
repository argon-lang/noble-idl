namespace NobleIDL.Runtime;

[AttributeUsage(AttributeTargets.Assembly, AllowMultiple = true)]
public class NobleIDLSourceFileAttribute : Attribute
{
    public NobleIDLSourceFileAttribute(string content) {
        Content = content;
    }
    
    public string Content { get; }
}
