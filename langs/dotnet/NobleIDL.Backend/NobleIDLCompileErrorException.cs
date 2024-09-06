namespace NobleIDL.Backend;

public class NobleIDLCompileErrorException : Exception {
    public NobleIDLCompileErrorException(string message) : base(message) { }

    public NobleIDLCompileErrorException(string message, Exception innerException) : base(message, innerException) { }
}