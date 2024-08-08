#!/bin/bash

cp noble-idl/runtime/nobleidl-core.nidl js/runtime/src/
cp noble-idl/example/example.nidl js/example/src/

mkdir -p java/runtime/src/main/resources/dev/argon/nobleidl/runtime/
cp noble-idl/runtime/nobleidl-core.nidl java/runtime/src/main/resources/dev/argon/nobleidl/runtime/

mkdir -p java/example/src/main/resources/dev/argon/nobleidl/example/
cp noble-idl/example/example.nidl java/example/src/main/nobleidl/dev/argon/nobleidl/example/

cp noble-idl/runtime/nobleidl-core.nidl rust/runtime/src/
cp noble-idl/example/example.nidl rust/example/src/
