use std::{collections::{HashMap, HashSet, VecDeque}, ffi::OsStr, path::{Path, PathBuf}};

use cargo_metadata::{CargoOpt, DependencyKind, MetadataCommand};

use crate::{CrateOptions, Crates, PackageMapping, RustIDLCompilerOptions, RustLanguageOptions};




#[derive(serde::Deserialize, Debug)]
struct NobleIDLRustMetadata {
    package_mapping: HashMap<String, String>,
}





pub fn load_options() -> RustIDLCompilerOptions {
    let mut manifest_path: PathBuf = std::env::var_os("CARGO_MANIFEST_DIR")
        .map(PathBuf::from)
        .unwrap_or_else(|| std::env::current_dir().unwrap());
    manifest_path.push("Cargo.toml");

    let metadata = MetadataCommand::new()
        .manifest_path(manifest_path)
        .features(CargoOpt::AllFeatures)
        .exec()
        .unwrap();

    let packages = &metadata.packages;
    let resolve = metadata.resolve.as_ref().expect("Failed to get dependency resolution");
    let resolve_nodes = &resolve.nodes;


    let mut input_dirs = HashSet::new();
    let mut library_dirs = HashSet::new();

    let mut root_crate_name = None;
    let mut crates = HashMap::new();

    // Traverse dependencies
    let mut to_scan = VecDeque::new();
    let mut visited_packages = HashSet::new();

    let root_crate = resolve.root.as_ref().expect("Could not get resolve.root.");
    to_scan.push_back(root_crate);

    while let Some(current_id) = to_scan.pop_front() {
        if visited_packages.contains(&current_id) {
            continue;
        }
        visited_packages.insert(current_id);

        let is_root_crate = current_id == root_crate;


        let resolve_node = resolve_nodes
            .iter()
            .find(|node| node.id == *current_id)
            .expect(&format!("Failed to get resolve node {}", current_id));

        for dep in &resolve_node.deps {
            if !dep.dep_kinds.iter().any(|kind| kind.kind == DependencyKind::Normal) {
                continue;
            }

            to_scan.push_back(&dep.pkg);
        }

        let package = packages
            .iter()
            .find(|pkg| pkg.id == *current_id)
            .expect(&format!("Failed to get package {}", current_id));

        let package_name = package.name.as_str();

        if is_root_crate {
            root_crate_name = Some(package_name);
        }

        if let Some(noble_idl_packages) = package.metadata.get("noble-idl")
            .and_then(|noble_idl| serde_json::from_value::<NobleIDLRustMetadata>(noble_idl.clone()).ok())
        {
            let crate_options = CrateOptions {
                package_mapping: PackageMapping {
                    package_mapping: noble_idl_packages.package_mapping,
                },
            };

            crates.insert(package_name.to_owned(), crate_options);



            add_idl_files(if is_root_crate { &mut input_dirs } else { &mut library_dirs }, package);
        }
    }

    let input_files = scan_dirs(&input_dirs);
    let library_files = scan_dirs(&library_dirs);

    let mut output_dir = PathBuf::from(std::env::var_os("OUT_DIR").expect("OUT_DIR not specified"));
    output_dir.push("noble_idl");

    RustIDLCompilerOptions {
        language_options: RustLanguageOptions {
            crate_name: root_crate_name.expect("Root crate name was not found").to_owned(),
            crates: Crates {
                crate_options: crates,
            },

            output_dir: output_dir.into_os_string().into_string().unwrap(),
        },

        input_files,
        library_files,
    }
}

fn add_idl_files<'a>(dirs: &mut HashSet<PathBuf>, package: &cargo_metadata::Package) {
    let Some(lib) = package.targets.iter().find(|target| target.is_lib()) else { return; };

    let path = PathBuf::from(&lib.src_path);
    let mut path = std::fs::canonicalize(path).expect("Could not canonicalize path.");
    path.pop();

    dirs.insert(path);

}

fn scan_dirs(dirs: &HashSet<PathBuf>) -> Vec<String> {
    let mut files = Vec::new();
    for dir in dirs {
        scan_dir(&mut files, dir)
    }
    files
}

fn scan_dir(files: &mut Vec<String>, dir: &Path) {
    for f in std::fs::read_dir(dir).unwrap() {
        let f = f.unwrap();

        if f.metadata().unwrap().is_dir() {
            scan_dir(files, &f.path());
        }
        else if f.path().extension() == Some(OsStr::new("nidl")) {
            files.push(f.path().to_str().unwrap().to_owned());
        }
    }
}

