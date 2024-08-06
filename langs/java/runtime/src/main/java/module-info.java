import dev.argon.nobleidl.runtime.NobleIDLSchema;

@NobleIDLSchema("dev/argon/nobleidl/runtime/schemas/nobleidl-core.nidl")
module dev.argon.nobleidl.runtime {
	exports dev.argon.nobleidl.runtime;
	opens dev.argon.nobleidl.runtime.schemas;
}