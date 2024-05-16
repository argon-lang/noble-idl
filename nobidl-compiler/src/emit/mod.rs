use std::fmt::Write;

use crate::model::Model;
use itertools::Itertools;

pub mod java;
pub mod scala;


#[derive(Debug)]
pub enum EmitOptions {
    Java(java::JavaEmitOptions),
    Scala(scala::ScalaEmitOptions),
    ScalaJS(scala::ScalaJSEmitOptions),
}

pub(crate) fn emit(model: &Model, options: &EmitOptions) -> Result<(), crate::Error> {
    match options {
        EmitOptions::Java(options) => java::emit(model, options),
        EmitOptions::Scala(options) => scala::emit_zio(model, options),
        EmitOptions::ScalaJS(options) => scala::emit_sjs(model, options),
    }
}


macro_rules! for_sep {
    ($item: pat, $coll: expr, $sep: block, $body: block) => {
        let mut iter = $coll.into_iter();
        if let Some($item) = iter.next() {
            $body;
            for $item in iter {
                $sep;
                $body;
            }
        }
    };
}

use for_sep;



fn name_to_pascal_case(name: &str) -> String {
    name.split('-')
        .map(|part| {
            let mut part = part.to_owned();
            let first = part.get_mut(0..1).unwrap();
            first.make_ascii_uppercase();
            part
        })
        .join("")
}

fn name_to_camel_case(name: &str) -> String {
    let mut name = name_to_pascal_case(name);
    let first = name.get_mut(0..1).unwrap();
    first.make_ascii_lowercase();
    name
}


struct IndentWriter<'a, W> {
    indent: &'a str,
    level: u32,
    needs_indent: bool,
    underlying: &'a mut W,
}

impl <'a, W: Write> IndentWriter<'a, W> {
    pub fn new(underlying: &'a mut W, indent: &'a str) -> Self {
        IndentWriter {
            indent,
            level: 0,
            needs_indent: true,
            underlying,
        }
    }

    pub fn indent(&mut self) {
        self.level = self.level.saturating_add(1);
    }

    pub fn dedent(&mut self) {
        self.level = self.level.saturating_sub(1);
    }
}

impl <'a, W: Write> Write for IndentWriter<'a, W> {
    fn write_str(&mut self, s: &str) -> std::fmt::Result {
        for part in Itertools::intersperse(s.split('\n'), "\n") {
            if part.is_empty() {
                continue;
            }

            if self.needs_indent {
                self.needs_indent = false;
                for _ in 0..self.level {
                    self.underlying.write_str(self.indent)?;
                }
            }

            self.underlying.write_str(part)?;
            if part == "\n" {
                self.needs_indent = true;
            }
        }

        Ok(())
    }
}

