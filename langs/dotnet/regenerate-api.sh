#!/bin/bash -e

cd "$(dirname "$0")"

pushd NobleIDL.Backend
dotnet run -- \
	-i ../../noble-idl/runtime/nobleidl-core.nidl \
	-o ../NobleIDL.Runtime/Gen.cs \
	--namespace nobleidl.core=NobleIDL.Runtime

popd

pushd NobleIDL.Runtime
dotnet build
popd

pushd NobleIDL.Backend
dotnet run -- \
	-i ../../noble-idl/backend/compiler-api.nidl \
	-i ../../noble-idl/backend/compiler-api-esexpr-annotations.nidl \
	--namespace nobleidl.compiler.api=NobleIDL.Backend.Api \
	--ref ../NobleIDL.Runtime/bin/Debug/net8.0/NobleIDL.Runtime.dll \
	-o Api.Gen.cs
popd


