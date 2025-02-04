use crate::ast;

use esexpr_text::parser::{simple_identifier, expr};

use nom::{
    IResult,
	Parser,
    branch::alt,
    character::complete::{alphanumeric1, multispace1},
    bytes::complete::{tag, take_until},
    combinator::{all_consuming, map, not, opt, value, cut},
    multi::{many0, many0_count, separated_list1},
    sequence::{delimited, pair, preceded, terminated},
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
	).parse(input)
}

fn comment(input: &str) -> IResult<&str, ()> {
	value(
		(),
		pair(
			tag("//"),
			take_until("\n"),
		),
	).parse(input)
}


fn sym(s: &'static str) -> impl Fn(&str) -> IResult<&str, &str> {
    move |input| preceded(skip_ws, tag(s)).parse(input)
}

fn keyword(s: &'static str) -> impl Fn(&str) -> IResult<&str, &str> {
    move |input| {
        terminated(
            preceded(
                skip_ws,
                tag(s)
            ),
            not(alphanumeric1)
        ).parse(input)
    }
}

fn identifier(input: &str) -> IResult<&str, &str> {
	preceded(
		skip_ws,
		simple_identifier
	).parse(input)
}


pub fn definition_file(input: &str) -> IResult<&str, ast::DefinitionFile> {
	all_consuming(
		map((
			package_specifier,
			many0(import),
			many0(definition),
			skip_ws,
		), |(package, imports, definitions, _)| {
			ast::DefinitionFile {
				package,
				imports,
				definitions,
			}
		})
	).parse(input)
}


fn package_name(input: &str) -> IResult<&str, ast::PackageName> {
    map(separated_list1(
        sym("."),
        map(identifier, str::to_owned),
    ), ast::PackageName).parse(input)
}

fn qual_name(input: &str) -> IResult<&str, ast::QualifiedName> {
    map(package_name, |mut package_name| {
        let name = package_name.0.pop().unwrap();
        ast::QualifiedName(Box::new(package_name), name)
    }).parse(input)
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
    }).parse(input)
}

fn import(input: &str) -> IResult<&str, ast::PackageName> {
    delimited(
        keyword("import"),
        package_name,
        sym(";"),
    ).parse(input)
}

fn annotations(input: &str) -> IResult<&str, Vec<ast::Annotation>> {
    many0(
        map((
            sym("@"),
            cut(identifier),
            cut(sym(":")),
			skip_ws,
            cut(expr),
        ), |(_, scope, _, _, value)| ast::Annotation { scope: scope.to_owned(), value })
    ).parse(input)
}



fn definition(input: &str) -> IResult<&str, ast::Definition> {
    alt((
        map(record_def, ast::Definition::Record),
        map(enum_def, ast::Definition::Enum),
        map(simple_enum_def, ast::Definition::SimpleEnum),
        map(extern_type, ast::Definition::ExternType),
        map(interface_def, ast::Definition::Interface),
		map(exception_type_def, ast::Definition::ExceptionType),
    )).parse(input)
}


fn record_def(input: &str) -> IResult<&str, ast::RecordDefinition> {
    map((
        annotations,
        keyword("record"),
        cut(identifier),
        type_parameters,
        cut(sym("{")),
        many0(record_field),
        cut(sym("}")),
    ), |(annotations, _, name, type_parameters, _, fields, _)| {
        ast::RecordDefinition {
            name: name.to_owned(),
            type_parameters,
            fields,
            annotations,
        }
    }).parse(input)
}

fn record_field(input: &str) -> IResult<&str, ast::RecordField> {
    map((
        annotations,
        identifier,
        cut(sym(":")),
        cut(type_expr),
        cut(sym(";")),
    ), |(annotations, name, _, field_type, _)| {
        ast::RecordField {
            name: name.to_owned(),
            field_type,
            annotations,
        }
    }).parse(input)
}

fn enum_def(input: &str) -> IResult<&str, ast::EnumDefinition> {
    map((
        annotations,
        keyword("enum"),
        cut(identifier),
        type_parameters,
        cut(sym("{")),
        separated_list1(sym(","), enum_case),
        opt(sym(",")),
        cut(sym("}")),
    ), |(annotations, _, name, type_parameters, _, cases, _, _)| {
        ast::EnumDefinition {
            name: name.to_owned(),
            type_parameters,
            cases,
            annotations,
        }
    }).parse(input)
}

fn enum_case(input: &str) -> IResult<&str, ast::EnumCase> {
    map((
        annotations,
        identifier,
        opt(enum_case_body),
    ), |(annotations, name, fields)| {
        ast::EnumCase {
            name: name.to_owned(),
            fields: fields.unwrap_or_default(),
            annotations,
        }
    }).parse(input)
}

fn enum_case_body(input: &str) -> IResult<&str, Vec<ast::RecordField>> {
    delimited(
        sym("{"),
        cut(many0(record_field)),
        cut(sym("}")),
    ).parse(input)
}

fn simple_enum_def(input: &str) -> IResult<&str, ast::SimpleEnumDefinition> {
    map((
        annotations,
        keyword("simple"),
        keyword("enum"),
        cut(identifier),
        cut(sym("{")),
        separated_list1(sym(","), simple_enum_case),
        opt(sym(",")),
        cut(sym("}")),
    ), |(annotations, _, _, name, _, cases, _, _)| {
        ast::SimpleEnumDefinition {
            name: name.to_owned(),
            cases,
            annotations,
        }
    }).parse(input)
}

fn simple_enum_case(input: &str) -> IResult<&str, ast::SimpleEnumCase> {
    map((
        annotations,
        identifier,
    ), |(annotations, name)| {
        ast::SimpleEnumCase {
            name: name.to_owned(),
            annotations,
        }
    }).parse(input)
}

fn extern_type(input: &str) -> IResult<&str, ast::ExternTypeDefinition> {
    map((
        annotations,
        keyword("extern"),
        keyword("type"),
        cut(identifier),
        type_parameters,
        sym(";"),
    ), |(annotations, _, _, name, type_parameters, _)| {
        ast::ExternTypeDefinition {
            name: name.to_owned(),
            type_parameters,
            annotations,
        }
    }).parse(input)
}

fn interface_def(input: &str) -> IResult<&str, ast::InterfaceDefinition> {
    map((
        annotations,
        keyword("interface"),
        cut(identifier),
        type_parameters,
        cut(sym("{")),
        many0(interface_method),
        cut(sym("}")),
    ), |(annotations, _, name, type_parameters, _, methods, _)| {
        ast::InterfaceDefinition {
            name: name.to_owned(),
            type_parameters,
            methods,
            annotations,
        }
    }).parse(input)
}

pub fn interface_method(input: &str) -> IResult<&str, ast::InterfaceMethod> {
    map((
        annotations,
        identifier,
        type_parameters,
        cut(sym("(")),
        method_parameters,
        cut(sym(")")),
        cut(sym(":")),
        type_expr,
		opt(preceded(
			keyword("throws"),
			cut(type_expr),
		)),
        cut(sym(";")),
    ), |(annotations, name, type_parameters, _, parameters, _, _, return_type, throws, _)| {
        ast::InterfaceMethod {
            name: name.to_owned(),
            type_parameters,
            annotations,
            parameters,
            return_type,
			throws,
        }
    }).parse(input)
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
    ), Option::unwrap_or_default).parse(input)
}

fn method_parameter(input: &str) -> IResult<&str, ast::InterfaceMethodParameter> {
    map((
        annotations,
        identifier,
        cut(sym(":")),
        type_expr,
    ), |(annotations, name, _, parameter_type)| {
        ast::InterfaceMethodParameter {
            name: name.to_owned(),
            parameter_type,
            annotations,
        }
    }).parse(input)
}

fn exception_type_def(input: &str) -> IResult<&str, ast::ExceptionTypeDefinition> {
    map((
        annotations,
        keyword("exception"),
        cut(identifier),
        keyword("of"),
        type_expr,
        sym(";"),
    ), |(annotations, _, name, _, information, _)| {
        ast::ExceptionTypeDefinition {
            name: name.to_owned(),
            information,
            annotations,
        }
    }).parse(input)
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
    ), Option::unwrap_or_default).parse(input)
}

fn type_parameter(input: &str) -> IResult<&str, ast::TypeParameter> {
	map(
		(
			annotations,
			identifier,
			opt(preceded(
				sym(":"),
				separated_list1(
					sym("+"),
					cut(constraint)
				)
			))
		),
		|(annotations, name, constraints)| ast::TypeParameter::Type {
			name: name.to_owned(),
			annotations: annotations.into_iter().map(Box::new).collect(),
			constraints: constraints.unwrap_or_default().into_iter().map(Box::new).collect(),
		}
	).parse(input)
}

fn constraint(input: &str) -> IResult<&str, ast::TypeParameterTypeConstraint> {
	map(
		keyword("exception"),
		|_| ast::TypeParameterTypeConstraint::Exception
	).parse(input)
}

fn type_expr(input: &str) -> IResult<&str, ast::TypeExpr> {
	map(
		pair(
			qual_name,
			opt(type_argument_list),
		),
		|(name, args)| ast::TypeExpr::UnresolvedName(name, args.unwrap_or_default())
	).parse(input)
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
    ).parse(input)
}


