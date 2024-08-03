use crate::ast;

use esexpr_text::parser::{simple_identifier, expr};

use nom::{
    IResult,
    branch::alt,
    character::complete::{alphanumeric1, multispace1},
    bytes::complete::{tag, take_until},
    combinator::{all_consuming, map, not, opt, value, cut},
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
	all_consuming(
		map(tuple((
			package_specifier,
			many0(import),
			many0(definition),
			skip_ws,
		)), |(package, imports, definitions, _)| {
			ast::DefinitionFile {
				package,
				imports,
				definitions,
			}
		})
	)(input)
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
            cut(package_name),
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
            cut(identifier),
            cut(sym(":")),
			skip_ws,
            cut(expr),
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
        cut(identifier),
        type_parameters,
        cut(sym("{")),
        many0(record_field),
        cut(sym("}")),
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
        cut(sym(":")),
        cut(type_expr),
        cut(sym(";")),
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
        cut(identifier),
        type_parameters,
        cut(sym("{")),
        separated_list1(sym(","), enum_case),
        opt(sym(",")),
        cut(sym("}")),
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
        opt(enum_case_body),
    )), |(annotations, name, fields)| {
        ast::EnumCase {
            name: name.to_owned(),
            fields: fields.unwrap_or_default(),
            annotations,
        }
    })(input)
}

fn enum_case_body(input: &str) -> IResult<&str, Vec<ast::RecordField>> {
    delimited(
        sym("{"),
        cut(many0(record_field)),
        cut(sym("}")),
    )(input)
}

fn extern_type(input: &str) -> IResult<&str, ast::ExternTypeDefinition> {
    map(tuple((
        annotations,
        keyword("extern"),
        keyword("type"),
        cut(identifier),
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
        cut(identifier),
        type_parameters,
        cut(sym("{")),
        many0(interface_method),
        cut(sym("}")),
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
        cut(sym("(")),
        method_parameters,
        cut(sym(")")),
        cut(sym(":")),
        type_expr,
        cut(sym(";")),
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
                cut(method_parameter),
            ),
            opt(sym(",")),
        ),
    ), Option::unwrap_or_default)(input)
}

fn method_parameter(input: &str) -> IResult<&str, ast::InterfaceMethodParameter> {
    map(tuple((
        annotations,
        identifier,
        cut(sym(":")),
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
                    type_parameter,
                ),
                opt(sym(","))
            ),
            cut(sym("]"))
        )
    ), Option::unwrap_or_default)(input)
}

fn type_parameter(input: &str) -> IResult<&str, ast::TypeParameter> {
	map(
		pair(
			annotations,
			identifier,
		),
		|(annotations, name)| ast::TypeParameter::Type { name: name.to_owned(), annotations }
	)(input)
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
        cut(terminated(
            separated_list1(
                sym(","),
                type_expr,
            ),
            opt(sym(","))
        )),
        cut(sym("]")),
    )(input)
}


