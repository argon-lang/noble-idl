use std::{path::Path, process::Command};





trait FileLoader {
    fn load(&mut self, name: &str, content: &str);
}




trait LanguageExecutor {
    fn setup<F: FileLoader>(&self, files: &mut F);
    fn execute(&self, temp_dir: &Path);
}


struct RustLanguage;

impl LanguageExecutor for RustLanguage {
    fn setup<F: FileLoader>(&self, files: &mut F) {
        files.load("Cargo.toml", &generate_cargo_toml());
        files.load("src/lib.rs", include_str!("compile_output/rust/src/lib.rs"));
        files.load("build.rs", include_str!("compile_output/rust/build.rs"));
        files.load("src/test.nidl", include_str!("compile_output/noble-idl/test.nidl"));
    }

    fn execute(&self, temp_dir: &Path) {
        let output = Command::new(std::env::var("CARGO").as_ref().map(String::as_str).unwrap_or("default"))
            .arg("test")
            .current_dir(temp_dir)
            .output()
            .unwrap();

        println!("{}", String::from_utf8_lossy(&output.stdout));
        eprintln!("{}", String::from_utf8_lossy(&output.stderr));

        assert!(output.status.success());
    }
}

fn generate_cargo_toml() -> String {
    use toml::Value;
    use toml::map::Map;

    let compiler_path = env!("CARGO_MANIFEST_DIR");


    let mut doc = toml::toml! {
        [package]
        name = "noble-idl-test"
        version = "0.1.0"
        edition = "2021"

        [dependencies]
        esexpr = "0.1.8"

        [package.metadata.noble-idl.package_mapping]
        "nobleidl.test" = ""
    };

    let deps = doc.get_mut("dependencies").unwrap().as_table_mut().unwrap();
    
    let mut noble_idl_runtime = Map::new();
    noble_idl_runtime.insert("path".to_owned(), Value::from(format!("{}/../runtime/rust/", compiler_path)));

    deps.insert("noble-idl-runtime".to_owned(), Value::from(noble_idl_runtime));

    let mut build_deps = Map::new();
    
    let mut noble_idl_compiler_rust = Map::new();
    noble_idl_compiler_rust.insert("path".to_owned(), Value::from(format!("{}/../noble-idl-compiler-rust", compiler_path)));

    build_deps.insert("noble-idl-compiler-rust".to_owned(), Value::from(noble_idl_compiler_rust));

    doc.insert("build-dependencies".to_owned(), Value::from(build_deps));

    let s = toml::to_string(&doc).unwrap();

    println!("Cargo.toml: {}", s);

    s
}



// trait OutputHandler {
//     fn static_files(&self, files: &mut HashMap<&'static str, &'static str>);
//     fn options(&self, temp_dir: &Path) -> Vec<EmitOptions>;
//     fn execute(&self, temp_dir: &Path);
// }


// struct JavaOutputHandler;

// impl OutputHandler for JavaOutputHandler {
//     fn static_files(&self, files: &mut HashMap<&'static str, &'static str>) {
//         files.insert("build.sbt", include_str!("compile_output/java/build.sbt"));
//         files.insert("src/main/java/dev/argon/nobidl/test/MyExtern.java", include_str!("compile_output/java/src/main/java/dev/argon/nobidl/test/MyExtern.java"));
//         files.insert("src/main/java/dev/argon/nobidl/test/MyExtern2.java", include_str!("compile_output/java/src/main/java/dev/argon/nobidl/test/MyExtern2.java"));
//     }

//     fn options(&self, temp_dir: &Path) -> Vec<EmitOptions> {
//         vec!(EmitOptions::Java(JavaEmitOptions {
//             package_mapping: HashMap::from([
//                 ("nobidl.core".to_owned(), "dev.argon.nobidl.core".to_owned()),
//                 ("nobidl.test".to_owned(), "dev.argon.nobidl.test".to_owned()),
//             ]),
//             output_dir: temp_dir.join("src/main/java"),
//             extern_type_mapping: java_type_mapping(),
//         }))
//     }

//     fn execute(&self, temp_dir: &Path) {
//         for file in std::fs::read_dir(temp_dir.join("src/main/java/dev/argon/nobidl/test/")).unwrap() {
//             let file = file.unwrap();
//             println!("{}:", file.file_name().to_string_lossy());
//             println!("{}", std::fs::read_to_string(file.path()).unwrap());
//         }
        
//         run_sbt(temp_dir);
//     }
// }



// fn java_type_mapping() -> HashMap<String, String> {
//     HashMap::from([
//         ("nobidl.core.string".to_owned(), "java.lang.String".to_owned()),
//         ("nobidl.core.binary".to_owned(), "byte[]".to_owned()),
//         ("nobidl.core.int".to_owned(), "java.math.BigInteger".to_owned()),
//         ("nobidl.core.nat".to_owned(), "java.math.BigInteger".to_owned()),
//         ("nobidl.core.i8".to_owned(), "byte".to_owned()),
//         ("nobidl.core.u8".to_owned(), "byte".to_owned()),
//         ("nobidl.core.i16".to_owned(), "short".to_owned()),
//         ("nobidl.core.u16".to_owned(), "short".to_owned()),
//         ("nobidl.core.i32".to_owned(), "int".to_owned()),
//         ("nobidl.core.u32".to_owned(), "int".to_owned()),
//         ("nobidl.core.i64".to_owned(), "long".to_owned()),
//         ("nobidl.core.u64".to_owned(), "long".to_owned()),
//         ("nobidl.core.f32".to_owned(), "float".to_owned()),
//         ("nobidl.core.f64".to_owned(), "double".to_owned()),
//         ("nobidl.core.unit".to_owned(), "void".to_owned()),
//         ("nobidl.core.list".to_owned(), "java.util.List".to_owned()),
//         ("nobidl.core.option".to_owned(), "java.util.Optional".to_owned()),
//     ])
// }

// fn run_sbt(temp_dir: &Path) {
//     let output = Command::new("sbt")
//         .arg("test")
//         .env("NOBIDL_ROOT_DIR", env!("CARGO_MANIFEST_DIR").to_owned() + "/..")
//         .current_dir(temp_dir)
//         .output()
//         .expect("Error running process");

//     println!("{}", String::from_utf8_lossy(&output.stdout));
//     println!("{}", String::from_utf8_lossy(&output.stderr));

//     assert!(output.status.success());
// }


// struct JavaOutputHandler;

// impl OutputHandler for JavaOutputHandler {
//     fn static_files(&self, files: &mut HashMap<&'static str, &'static str>) {
//         files.insert("build.sbt", include_str!("compile_output/java/build.sbt"));
//         files.insert("src/main/java/dev/argon/nobidl/test/MyExtern.java", include_str!("compile_output/java/src/main/java/dev/argon/nobidl/test/MyExtern.java"));
//         files.insert("src/main/java/dev/argon/nobidl/test/MyExtern2.java", include_str!("compile_output/java/src/main/java/dev/argon/nobidl/test/MyExtern2.java"));
//     }

//     fn options(&self, temp_dir: &Path) -> Vec<EmitOptions> {
//         vec!(EmitOptions::Java(JavaEmitOptions {
//             package_mapping: HashMap::from([
//                 ("nobidl.core".to_owned(), "dev.argon.nobidl.core".to_owned()),
//                 ("nobidl.test".to_owned(), "dev.argon.nobidl.test".to_owned()),
//             ]),
//             output_dir: temp_dir.join("src/main/java"),
//             extern_type_mapping: java_type_mapping(),
//         }))
//     }

//     fn execute(&self, temp_dir: &Path) {
//         for file in std::fs::read_dir(temp_dir.join("src/main/java/dev/argon/nobidl/test/")).unwrap() {
//             let file = file.unwrap();
//             println!("{}:", file.file_name().to_string_lossy());
//             println!("{}", std::fs::read_to_string(file.path()).unwrap());
//         }
        
//         run_sbt(temp_dir);
//     }
// }

// struct ScalaOutputHandler;

// impl OutputHandler for ScalaOutputHandler {
//     fn static_files(&self, files: &mut HashMap<&'static str, &'static str>) {
//         files.insert("build.sbt", include_str!("compile_output/scala/zio/build.sbt"));
//         files.insert("MyExtern.scala", include_str!("compile_output/scala/zio/MyExtern.scala"));
//     }

//     fn options(&self, temp_dir: &Path) -> Vec<EmitOptions> {
//         vec!(EmitOptions::Scala(scala_zio_emit_options(temp_dir)))
//     }

//     fn execute(&self, temp_dir: &Path) {
//         println!("{}", std::fs::read_to_string(temp_dir.join("output_zio.scala")).unwrap());
//         run_sbt(temp_dir);
//     }
// }

// fn scala_zio_emit_options(temp_dir: &Path) -> emit::scala::ScalaEmitOptions {
//     ScalaEmitOptions {
//         package_mapping: HashMap::from([
//             ("nobidl.core".to_owned(), "dev.argon.nobidl.scala_zio.core".to_owned()),
//             ("nobidl.test".to_owned(), "dev.argon.nobidl.scala_zio.test".to_owned()),
//         ]),
//         output_file: temp_dir.join("output_zio.scala"),
//     }
// }

// struct ScalaJSOutputHandler;

// impl OutputHandler for ScalaJSOutputHandler {
//     fn static_files(&self, files: &mut HashMap<&'static str, &'static str>) {
//         files.insert("build.sbt", include_str!("compile_output/scala/sjs/build.sbt"));
//         files.insert("project/plugins.sbt", include_str!("compile_output/scala/sjs/project/plugins.sbt"));
//         files.insert("MyExternJS.scala", include_str!("compile_output/scala/sjs/MyExtern.scala"));
//         files.insert("MyExternZIO.scala", include_str!("compile_output/scala/zio/MyExtern.scala"));
//     }

//     fn options(&self, temp_dir: &Path) -> Vec<EmitOptions> {
//         vec!(EmitOptions::ScalaJS(scala_js_emit_options(temp_dir)))
//     }

//     fn execute(&self, temp_dir: &Path) {
//         println!("{}", std::fs::read_to_string(temp_dir.join("output_sjs.scala")).unwrap());
//         run_sbt(temp_dir);
//     }
// }

// fn scala_js_emit_options(temp_dir: &Path) -> emit::scala::ScalaJSEmitOptions {
//     ScalaJSEmitOptions {
//         package_mapping: HashMap::from([
//             ("nobidl.core".to_owned(), "dev.argon.nobidl.sjs.core".to_owned()),
//             ("nobidl.test".to_owned(), "dev.argon.nobidl.sjs.test".to_owned()),
//         ]),
//         output_file: temp_dir.join("output_sjs.scala"),
//         scala_zio_package_mapping: None,
//     }
// }


// struct ScalaJSAdapterOutputHandler;


// impl OutputHandler for ScalaJSAdapterOutputHandler {
//     fn static_files(&self, files: &mut HashMap<&'static str, &'static str>) {
//         files.insert("build.sbt", include_str!("compile_output/scala/sjs/build.sbt"));
//         files.insert("project/plugins.sbt", include_str!("compile_output/scala/sjs/project/plugins.sbt"));
//         files.insert("MyExternJS.scala", include_str!("compile_output/scala/sjs/MyExtern.scala"));
//         files.insert("MyExternZIO.scala", include_str!("compile_output/scala/zio/MyExtern.scala"));
//     }

//     fn options(&self, temp_dir: &Path) -> Vec<EmitOptions> {
//         let zio_options = scala_zio_emit_options(temp_dir);
//         let mut sjs_options = scala_js_emit_options(temp_dir);
//         sjs_options.scala_zio_package_mapping = Some(zio_options.package_mapping.clone());
//         vec!(
//             EmitOptions::Scala(zio_options),
//             EmitOptions::ScalaJS(sjs_options),
//         )
//     }

//     fn execute(&self, temp_dir: &Path) {
//         println!("{}", std::fs::read_to_string(temp_dir.join("output_sjs.scala")).unwrap());
//         println!("{}", std::fs::read_to_string(temp_dir.join("output_sjs.scala")).unwrap());
//         run_sbt(temp_dir);
//     }
// }


struct TempDirFileLoader<'a> {
    dir: &'a Path,
}

impl <'a> FileLoader for TempDirFileLoader<'a> {
    fn load(&mut self, name: &str, content: &str) {
        let mut path = self.dir.to_owned();
        path.push(name);
        std::fs::create_dir_all(path.parent().unwrap()).unwrap();
        std::fs::write(path, content).unwrap();
    }
}


fn execute_lang<L: LanguageExecutor + std::panic::RefUnwindSafe>(lang: &L) {
    let temp_dir = tempfile::tempdir().unwrap();
    
    let mut files = TempDirFileLoader {
        dir: temp_dir.path(),
    };

    lang.setup(&mut files);

    let res = std::panic::catch_unwind(|| {
        lang.execute(temp_dir.path())
    });
    
    
    for f in walkdir::WalkDir::new(temp_dir.path()) {
        let f = f.unwrap();
        let f = f.path();

        if f.is_dir() {
            continue;
        }

        if !f.extension().is_some_and(|ext| ext == "rs" || ext == "toml") {
            continue;
        }


        println!("{:?}", f);
    }

    match res {
        Ok(_) => {},
        Err(e) => std::panic::resume_unwind(e),
    }
}

#[test]
fn test_execute_rust() {
    execute_lang(&RustLanguage);
}

// #[test]
// fn test_execute_java() {
//     execute_output(&JavaOutputHandler);
// }

// #[test]
// fn test_execute_scala() {
//     execute_output(&ScalaOutputHandler);
// }

// #[test]
// fn test_execute_scalajs() {
//     execute_output(&ScalaJSOutputHandler);
// }

// #[test]
// fn test_execute_scalajs_adapter() {
//     execute_output(&ScalaJSAdapterOutputHandler);
// }


