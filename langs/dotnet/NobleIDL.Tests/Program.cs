namespace NobleIDL.Tests;


public static class Program {

    public static void Main() {
        Console.WriteLine("Hello, World!" + new Either<int, string>.Left { Value = 4 });
    }

}
