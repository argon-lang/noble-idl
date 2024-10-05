#!/bin/bash

cp noble-idl/runtime/nobleidl-core.nidl js/runtime/src/
cp noble-idl/test/test.nidl js/test/src/

mkdir -p java/runtime/src/main/resources/nobleidl
cp noble-idl/runtime/nobleidl-core.nidl java/runtime/src/main/resources/nobleidl/

mkdir -p java/test/src/main/nobleidl/dev/argon/nobleidl/test/
cp noble-idl/test/test.nidl java/test/src/main/nobleidl/dev/argon/nobleidl/test/

mkdir -p scala/runtime/shared/src/main/resources/nobleidl/core/
cp noble-idl/runtime/nobleidl-core.nidl scala/runtime/shared/src/main/resources/nobleidl/core/

mkdir -p scala/test/shared/src/main/nobleidl/nobleidl/test/
cp noble-idl/test/test.nidl scala/test/shared/src/main/nobleidl/nobleidl/test/

cp noble-idl/test/test.nidl dotnet/NobleIDL.Tests/

cp noble-idl/runtime/nobleidl-core.nidl rust/runtime/src/
cp noble-idl/test/test.nidl rust/test/src/
