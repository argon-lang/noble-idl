use crate::ast;

use esexpr_text::parser::{simple_identifier, expr};

use nom::{
    IResult,
    branch::alt,
    character::complete::{alphanumeric1, multispace1},
    bytes::complete::{tag, take_until},
    combinator::{eof, map, not, opt, value},
    multi::{many0, many0_count, separated_list1},
    sequence::{delimited, pair, preceded, terminated, tuple},
};


fn skip_ws(input: &str) -> IResult<&str, ()> {
	value(
		(),
		many0_count(
			alt((
				value((), multispace1),
				comment,
			))
		),
	)(input)
}

fn comment(input: &str) -> IResult<&str, ()> {
	value(
		(),
		pair(
			tag("//"),
			take_until("\n"),
		),
	)(input)
}


fn sym(s: &'static str) -> impl Fn(&str) -> IResult<&str, &str> {
    move |input| preceded(skip_ws, tag(s))(input)
}

fn keyword(s: &'static str) -> impl Fn(&str) -> IResult<&str, &str> {
    move |input| {
        terminated(
            preceded(
                skip_ws,
                tag(s)
            ),
            not(alphanumeric1)
        )(input)
    }
}

fn identifier(input: &str) -> IResult<&str, &str> {
	preceded(
		skip_ws,
		simple_identifier
	)(input)
}


pub fn definition_file(input: &str) -> IResult<&str, ast::DefinitionFile> {
    map(tuple((
        package_specifier,
        many0(import),
        many0(definition),
        skip_ws,
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
        map(identifier, str::to_owned),
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
            identifier,
            sym(":"),
			skip_ws,
            expr,
        )), |(_, scope, _, _, value)| ast::Annotation { scope: scope.to_owned(), value })
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
        identifier,
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
        identifier,
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
        identifier,
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
        identifier,
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
        identifier,
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
        identifier,
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
        identifier,
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
        identifier,
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
                    map(identifier, |name| ast::TypeParameter::Type(name.to_owned())),
                ),
                opt(sym(","))
            ),
            sym("]")
        )
    ), Option::unwrap_or_default)(input)
}


fn type_expr(input: &str) -> IResult<&str, ast::TypeExpr> {
	map(
		pair(
			qual_name,
			opt(type_argument_list),
		),
		|(name, args)| ast::TypeExpr::UnresolvedName(name, args.unwrap_or_default())
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


