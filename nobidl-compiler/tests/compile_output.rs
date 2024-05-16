use std::path::PathBuf;
use std::{path::Path, process::Command};
use std::collections::HashMap;

use nobidl_compiler::emit;
use nobidl_compiler::emit::scala::ScalaJSEmitOptions;
use nobidl_compiler::{
    NobIDLOptions,
    compile,
    emit::{
        EmitOptions,
        java::JavaEmitOptions,
        scala::ScalaEmitOptions,
    },
};



trait OutputHandler {
    fn static_files(&self, files: &mut HashMap<&'static str, &'static str>);
    fn options(&self, temp_dir: &Path) -> Vec<EmitOptions>;
    fn execute(&self, temp_dir: &Path);
}


fn java_type_mapping() -> HashMap<String, String> {
    HashMap::from([
        ("nobidl.core.string".to_owned(), "java.lang.String".to_owned()),
        ("nobidl.core.binary".to_owned(), "byte[]".to_owned()),
        ("nobidl.core.int".to_owned(), "java.math.BigInteger".to_owned()),
        ("nobidl.core.nat".to_owned(), "java.math.BigInteger".to_owned()),
        ("nobidl.core.i8".to_owned(), "byte".to_owned()),
        ("nobidl.core.u8".to_owned(), "byte".to_owned()),
        ("nobidl.core.i16".to_owned(), "short".to_owned()),
        ("nobidl.core.u16".to_owned(), "short".to_owned()),
        ("nobidl.core.i32".to_owned(), "int".to_owned()),
        ("nobidl.core.u32".to_owned(), "int".to_owned()),
        ("nobidl.core.i64".to_owned(), "long".to_owned()),
        ("nobidl.core.u64".to_owned(), "long".to_owned()),
        ("nobidl.core.f32".to_owned(), "float".to_owned()),
        ("nobidl.core.f64".to_owned(), "double".to_owned()),
        ("nobidl.core.unit".to_owned(), "void".to_owned()),
        ("nobidl.core.list".to_owned(), "java.util.List".to_owned()),
        ("nobidl.core.option".to_owned(), "java.util.Optional".to_owned()),
    ])
}

fn run_sbt(temp_dir: &Path) {
    let output = Command::new("sbt")
        .arg("test")
        .env("NOBIDL_ROOT_DIR", env!("CARGO_MANIFEST_DIR").to_owned() + "/..")
        .current_dir(temp_dir)
        .output()
        .expect("Error running process");

    println!("{}", String::from_utf8_lossy(&output.stdout));
    println!("{}", String::from_utf8_lossy(&output.stderr));

    assert!(output.status.success());
}


struct JavaOutputHandler;

impl OutputHandler for JavaOutputHandler {
    fn static_files(&self, files: &mut HashMap<&'static str, &'static str>) {
        files.insert("build.sbt", include_str!("compile_output/java/build.sbt"));
        files.insert("src/main/java/dev/argon/nobidl/test/MyExtern.java", include_str!("compile_output/java/src/main/java/dev/argon/nobidl/test/MyExtern.java"));
        files.insert("src/main/java/dev/argon/nobidl/test/MyExtern2.java", include_str!("compile_output/java/src/main/java/dev/argon/nobidl/test/MyExtern2.java"));
    }

    fn options(&self, temp_dir: &Path) -> Vec<EmitOptions> {
        vec!(EmitOptions::Java(JavaEmitOptions {
            package_mapping: HashMap::from([
                ("nobidl.core".to_owned(), "dev.argon.nobidl.core".to_owned()),
                ("nobidl.test".to_owned(), "dev.argon.nobidl.test".to_owned()),
            ]),
            output_dir: temp_dir.join("src/main/java"),
            extern_type_mapping: java_type_mapping(),
        }))
    }

    fn execute(&self, temp_dir: &Path) {
        for file in std::fs::read_dir(temp_dir.join("src/main/java/dev/argon/nobidl/test/")).unwrap() {
            let file = file.unwrap();
            println!("{}:", file.file_name().to_string_lossy());
            println!("{}", std::fs::read_to_string(file.path()).unwrap());
        }
        
        run_sbt(temp_dir);
    }
}

struct ScalaOutputHandler;

impl OutputHandler for ScalaOutputHandler {
    fn static_files(&self, files: &mut HashMap<&'static str, &'static str>) {
        files.insert("build.sbt", include_str!("compile_output/scala/zio/build.sbt"));
        files.insert("MyExtern.scala", include_str!("compile_output/scala/zio/MyExtern.scala"));
    }

    fn options(&self, temp_dir: &Path) -> Vec<EmitOptions> {
        vec!(EmitOptions::Scala(scala_zio_emit_options(temp_dir)))
    }

    fn execute(&self, temp_dir: &Path) {
        println!("{}", std::fs::read_to_string(temp_dir.join("output_zio.scala")).unwrap());
        run_sbt(temp_dir);
    }
}

fn scala_zio_emit_options(temp_dir: &Path) -> emit::scala::ScalaEmitOptions {
    ScalaEmitOptions {
        package_mapping: HashMap::from([
            ("nobidl.core".to_owned(), "dev.argon.nobidl.scala_zio.core".to_owned()),
            ("nobidl.test".to_owned(), "dev.argon.nobidl.scala_zio.test".to_owned()),
        ]),
        output_file: temp_dir.join("output_zio.scala"),
    }
}

struct ScalaJSOutputHandler;

impl OutputHandler for ScalaJSOutputHandler {
    fn static_files(&self, files: &mut HashMap<&'static str, &'static str>) {
        files.insert("build.sbt", include_str!("compile_output/scala/sjs/build.sbt"));
        files.insert("project/plugins.sbt", include_str!("compile_output/scala/sjs/project/plugins.sbt"));
        files.insert("MyExternJS.scala", include_str!("compile_output/scala/sjs/MyExtern.scala"));
        files.insert("MyExternZIO.scala", include_str!("compile_output/scala/zio/MyExtern.scala"));
    }

    fn options(&self, temp_dir: &Path) -> Vec<EmitOptions> {
        vec!(EmitOptions::ScalaJS(scala_js_emit_options(temp_dir)))
    }

    fn execute(&self, temp_dir: &Path) {
        println!("{}", std::fs::read_to_string(temp_dir.join("output_sjs.scala")).unwrap());
        run_sbt(temp_dir);
    }
}

fn scala_js_emit_options(temp_dir: &Path) -> emit::scala::ScalaJSEmitOptions {
    ScalaJSEmitOptions {
        package_mapping: HashMap::from([
            ("nobidl.core".to_owned(), "dev.argon.nobidl.sjs.core".to_owned()),
            ("nobidl.test".to_owned(), "dev.argon.nobidl.sjs.test".to_owned()),
        ]),
        output_file: temp_dir.join("output_sjs.scala"),
        scala_zio_package_mapping: None,
    }
}


struct ScalaJSAdapterOutputHandler;


impl OutputHandler for ScalaJSAdapterOutputHandler {
    fn static_files(&self, files: &mut HashMap<&'static str, &'static str>) {
        files.insert("build.sbt", include_str!("compile_output/scala/sjs/build.sbt"));
        files.insert("project/plugins.sbt", include_str!("compile_output/scala/sjs/project/plugins.sbt"));
        files.insert("MyExternJS.scala", include_str!("compile_output/scala/sjs/MyExtern.scala"));
        files.insert("MyExternZIO.scala", include_str!("compile_output/scala/zio/MyExtern.scala"));
    }

    fn options(&self, temp_dir: &Path) -> Vec<EmitOptions> {
        let zio_options = scala_zio_emit_options(temp_dir);
        let mut sjs_options = scala_js_emit_options(temp_dir);
        sjs_options.scala_zio_package_mapping = Some(zio_options.package_mapping.clone());
        vec!(
            EmitOptions::Scala(zio_options),
            EmitOptions::ScalaJS(sjs_options),
        )
    }

    fn execute(&self, temp_dir: &Path) {
        println!("{}", std::fs::read_to_string(temp_dir.join("output_sjs.scala")).unwrap());
        println!("{}", std::fs::read_to_string(temp_dir.join("output_sjs.scala")).unwrap());
        run_sbt(temp_dir);
    }
}




fn execute_output<Handler: OutputHandler>(handler: &Handler) {
    let temp_dir = tempfile::tempdir().unwrap();
    

    let options: NobIDLOptions = NobIDLOptions {
        files: vec!(
            PathBuf::from(env!("CARGO_MANIFEST_DIR").to_owned() + "/tests/compile_output/nobidl/test.nidl"),
        ),
        library_files: vec!(
            PathBuf::from(env!("CARGO_MANIFEST_DIR").to_owned() + "/../runtime/nobidl/nobidl-core.nidl"),
        ),
        emit: handler.options(temp_dir.path()),
    };

    compile(&options).unwrap();



    let mut files = HashMap::new();
    handler.static_files(&mut files);

    for (file_name, text) in files {
        let mut path = temp_dir.path().to_owned();
        path.push(file_name);

        std::fs::create_dir_all(path.parent().unwrap()).unwrap();
        std::fs::write(path, text).unwrap();
    }

    handler.execute(temp_dir.path());


}


#[test]
fn test_execute_java() {
    execute_output(&JavaOutputHandler);
}

#[test]
fn test_execute_scala() {
    execute_output(&ScalaOutputHandler);
}

#[test]
fn test_execute_scalajs() {
    execute_output(&ScalaJSOutputHandler);
}

#[test]
fn test_execute_scalajs_adapter() {
    execute_output(&ScalaJSAdapterOutputHandler);
}


