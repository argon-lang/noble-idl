use crate::ast;

use esexpr_text::parser::{simple_identifier, expr};

use nom::{
    IResult,
    branch::alt,
    character::complete::{alphanumeric1, multispace0},
    bytes::complete::tag,
    combinator::{eof, flat_map, map, not, opt},
    multi::{fold_many0, many0, separated_list1},
    sequence::{delimited, preceded, terminated, tuple},
};

fn sym(s: &'static str) -> impl Fn(&str) -> IResult<&str, &str> {
    move |input| preceded(multispace0, tag(s))(input)
}

fn keyword(s: &'static str) -> impl Fn(&str) -> IResult<&str, &str> {
    move |input| {
        terminated(
            preceded(
                multispace0,
                tag(s)
            ),
            not(alphanumeric1)
        )(input)
    }
}


pub fn definition_file(input: &str) -> IResult<&str, ast::DefinitionFile> {
    map(tuple((
        package_specifier,
        many0(import),
        many0(definition),
        multispace0,
        eof
    )), |(package, imports, definitions, _, _)| {
        ast::DefinitionFile {
            package,
            imports,
            definitions,
        }
    })(input)
}


fn package_name(input: &str) -> IResult<&str, ast::PackageName> {
    map(separated_list1(
        sym("."),
        map(simple_identifier, str::to_owned),
    ), ast::PackageName)(input)
}

fn qual_name(input: &str) -> IResult<&str, ast::QualifiedName> {
    map(package_name, |mut package_name| {
        let name = package_name.0.pop().unwrap();
        ast::QualifiedName(package_name, name)
    })(input)
}

fn package_specifier(input: &str) -> IResult<&str, ast::PackageName> {
    map(opt(delimited(
            keyword("package"),
            package_name,
            sym(";"),
    )), |pkg| {
        match pkg {
            Some(pkg) => pkg,
            None => ast::PackageName(vec!()),
        }
    })(input)
}

fn import(input: &str) -> IResult<&str, ast::PackageName> {
    delimited(
        keyword("import"),
        package_name,
        sym(";"),
    )(input)
}

fn annotations(input: &str) -> IResult<&str, Vec<ast::Annotation>> {
    many0(
        map(tuple((
            sym("@"),
            simple_identifier,
            sym(":"),
            expr,
        )), |(_, scope, _, value)| ast::Annotation { scope: scope.to_owned(), value })
    )(input)
}



fn definition(input: &str) -> IResult<&str, ast::Definition> {
    alt((
        map(record_def, ast::Definition::Record),
        map(enum_def, ast::Definition::Enum),
        map(extern_type, ast::Definition::ExternType),
        map(interface_def, ast::Definition::Interface),
    ))(input)
}


fn record_def(input: &str) -> IResult<&str, ast::RecordDefinition> {
    map(tuple((
        annotations,
        keyword("record"),
        simple_identifier,
        type_parameters,
        sym("{"),
        many0(record_field),
        sym("}"),
    )), |(annotations, _, name, type_parameters, _, fields, _)| {
        ast::RecordDefinition {
            name: name.to_owned(),
            type_parameters,
            fields,
            annotations,
        }
    })(input)
}

fn record_field(input: &str) -> IResult<&str, ast::RecordField> {
    map(tuple((
        annotations,
        simple_identifier,
        sym(":"),
        type_expr,
        sym(";"),
    )), |(annotations, name, _, field_type, _)| {
        ast::RecordField {
            name: name.to_owned(),
            field_type,
            annotations,
        }
    })(input)
}

fn enum_def(input: &str) -> IResult<&str, ast::EnumDefinition> {
    map(tuple((
        annotations,
        keyword("enum"),
        simple_identifier,
        type_parameters,
        sym("{"),
        separated_list1(sym(","), enum_case),
        opt(sym(",")),
        sym("}"),
    )), |(annotations, _, name, type_parameters, _, cases, _, _)| {
        ast::EnumDefinition {
            name: name.to_owned(),
            type_parameters,
            cases,
            annotations,
        }
    })(input)
}

fn enum_case(input: &str) -> IResult<&str, ast::EnumCase> {
    map(tuple((
        annotations,
        simple_identifier,
        sym("{"),
        many0(record_field),
        sym("}"),
    )), |(annotations, name, _, fields, _)| {
        ast::EnumCase {
            name: name.to_owned(),
            fields,
            annotations,
        }
    })(input)
}

fn extern_type(input: &str) -> IResult<&str, ast::ExternTypeDefinition> {
    map(tuple((
        annotations,
        keyword("extern"),
        keyword("type"),
        simple_identifier,
        type_parameters,
        sym(";"),
    )), |(annotations, _, _, name, type_parameters, _)| {
        ast::ExternTypeDefinition {
            name: name.to_owned(),
            type_parameters,
            annotations,
        }
    })(input)
}

fn interface_def(input: &str) -> IResult<&str, ast::InterfaceDefinition> {
    map(tuple((
        annotations,
        keyword("interface"),
        simple_identifier,
        type_parameters,
        sym("{"),
        many0(interface_method),
        sym("}"),
    )), |(annotations, _, name, type_parameters, _, methods, _)| {
        ast::InterfaceDefinition {
            name: name.to_owned(),
            type_parameters,
            methods,
            annotations,
        }
    })(input)
}

pub fn interface_method(input: &str) -> IResult<&str, ast::InterfaceMethod> {
    map(tuple((
        annotations,
        simple_identifier,
        type_parameters,
        sym("("),
        method_parameters,
        sym(")"),
        sym(":"),
        type_expr,
        sym(";"),
    )), |(annotations, name, type_parameters, _, parameters, _, _, return_type, _)| {
        ast::InterfaceMethod {
            name: name.to_owned(),
            type_parameters,
            annotations,
            parameters,
            return_type,
        }
    })(input)
}

fn method_parameters(input: &str) -> IResult<&str, Vec<ast::InterfaceMethodParameter>> {
    map(opt(
        terminated(
            separated_list1(
                sym(","),
                method_parameter,
            ),
            opt(sym(",")),
        ),
    ), Option::unwrap_or_default)(input)
}

fn method_parameter(input: &str) -> IResult<&str, ast::InterfaceMethodParameter> {
    map(tuple((
        annotations,
        simple_identifier,
        sym(":"),
        type_expr,
    )), |(annotations, name, _, parameter_type)| {
        ast::InterfaceMethodParameter {
            name: name.to_owned(),
            parameter_type,
            annotations,
        }
    })(input)
}

fn type_parameters(input: &str) -> IResult<&str, Vec<ast::TypeParameter>> {
    map(opt(
        delimited(
            sym("["),
            terminated(
                separated_list1(
                    sym(","),
                    map(simple_identifier, |name| ast::TypeParameter::Type(name.to_owned())),
                ),
                opt(sym(","))
            ),
            sym("]")
        )
    ), Option::unwrap_or_default)(input)
}


enum TypeSuffix {
    TypeArguments(Vec<ast::TypeExpr>),
}

fn type_expr(input: &str) -> IResult<&str, ast::TypeExpr> {
    flat_map(
        map(qual_name, ast::TypeExpr::UnresolvedName),
        |base_expr| fold_many0(
            map(type_argument_list, TypeSuffix::TypeArguments),
            move || base_expr.clone(),
            |base_expr, suffix| match suffix {
                TypeSuffix::TypeArguments(args) => ast::TypeExpr::Apply(Box::new(base_expr), args),
            }),
    )(input)
}

fn type_argument_list(input: &str) -> IResult<&str, Vec<ast::TypeExpr>> {
    delimited(
        sym("["),
        terminated(
            separated_list1(
                sym(","),
                type_expr,
            ),
            opt(sym(","))
        ),
        sym("]"),
    )(input)
}


