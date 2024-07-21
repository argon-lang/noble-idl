
use logos::{Logos, SpannedIter};


#[derive(Default, Debug, Clone, PartialEq)]
pub enum LexerError {
    #[default]
    InvalidToken,
}


#[derive(Logos, Debug, PartialEq, Eq, Clone)]
#[logos(skip r"\s+")]
#[logos(error = LexerError)]
pub(crate) enum Token {
    #[regex(r"[a-z]|[a-z][a-z0-9-]*[a-z0-9]", |id| id.slice().to_owned())]
    Identifier(String),

    #[token("package")]
    KwPackage,

    #[token("import")]
    KwImport,

    #[token("record")]
    KwRecord,

    #[token("enum")]
    KwEnum,

    #[token("interface")]
    KwInterface,

    #[token("extern")]
    KwExtern,

    #[token("type")]
    KwType,

    #[token(";")]
    SymSemicolon,

    #[token(":")]
    SymColon,

    #[token(".")]
    SymDot,

    #[token(",")]
    SymComma,

    #[token("{")]
    SymOpenCurly,

    #[token("}")]
    SymCloseCurly,

    #[token("(")]
    SymOpenParen,

    #[token(")")]
    SymCloseParen,

    #[token("[")]
    SymOpenSquare,

    #[token("]")]
    SymCloseSquare,
}


pub(crate) type Spanned<Tok, Loc, Error> = Result<(Loc, Tok, Loc), Error>;
pub(crate) struct Lexer<'input> {
    token_stream: SpannedIter<'input, Token>,
}

impl<'input> Lexer<'input> {
    pub fn new(input: &'input str) -> Self {
      Self { token_stream: Token::lexer(input).spanned() }
    }
}

impl<'input> Iterator for Lexer<'input> {
    type Item = Spanned<Token, usize, LexerError>;

    fn next(&mut self) -> Option<Self::Item> {
        self.token_stream
        .next()
        .map(|(token, span)| Ok((span.start, token?, span.end)))
    }
}
