%%
%class Lexer
%unicode
%line
%column

%{
  private java_cup.runtime.Symbol symbol(int type) {
    return new java_cup.runtime.Symbol(type, yyline, yycolumn);
  }
  
  private java_cup.runtime.Symbol symbol(int type, Object value) {
    return new java_cup.runtime.Symbol(type, yyline, yycolumn, value);
  }
%}

%{
// Macros
WHITESPACE = [ \t\r\n]+
IDENTIFIER = [A-Za-z_][A-Za-z0-9_]*
NUMBER     = [0-9]+
STRING     = \"([^\"\\]|\\.)*\"
COMMENT    = "//".*

%%

{WHITESPACE}     { /* skip */ }
{COMMENT}        { /* skip */ }

// Reserved words
"config"         { return symbol(sym.CONFIG); }
"base_url"       { return symbol(sym.BASE_URL); }
"header"         { return symbol(sym.HEADER); }
"let"            { return symbol(sym.LET); }
"test"           { return symbol(sym.TEST); }
"GET"            { return symbol(sym.GET); }
"POST"           { return symbol(sym.POST); }
"PUT"            { return symbol(sym.PUT); }
"DELETE"         { return symbol(sym.DELETE); }
"expect"         { return symbol(sym.EXPECT); }
"status"         { return symbol(sym.STATUS); }
"body"           { return symbol(sym.BODY); }
"contains"       { return symbol(sym.CONTAINS); }

// Identifiers, Numbers, Strings
{IDENTIFIER}     { return symbol(sym.IDENTIFIER, yytext()); }
{NUMBER}         { return symbol(sym.NUMBER, Integer.parseInt(yytext())); }
{STRING}         { return symbol(sym.STRING, yytext()); }

// Symbols
"{"              { return symbol(sym.LBRACE); }
"}"              { return symbol(sym.RBRACE); }
"("              { return symbol(sym.LPAREN); }
")"              { return symbol(sym.RPAREN); }
"="              { return symbol(sym.EQ); }
";"              { return symbol(sym.SEMICOLON); }
"."              { return symbol(sym.DOT); }
".."             { return symbol(sym.DOTDOT); }

// Catch-all
.                { System.err.println("Unknown character: " + yytext()); }

