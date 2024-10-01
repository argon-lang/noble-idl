# NobleIDL MSBuild Task

Generates C# sources from Noble IDL schemas.

## Configuration

Add additional `.nidl` files from outside the project.

```xml
<ItemGroup>
  <NobleIDL Include="..." />
</ItemGroup>
```


Provide namespace mapping for Noble IDL packages.

```xml
<ItemGroup>
  <NobleIDLPackage Include="nobleidl.example">
    <Namespace>NobleIDL.Example</Namespace>
  </NobleIDLPackage>
</ItemGroup>
```

