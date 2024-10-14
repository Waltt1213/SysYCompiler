package frontend;

import java.util.ArrayList;
import java.util.HashMap;

public class Lexer {
    private int tokenIndex;
    private int lineno;
    private final String sourceCode;
    private final ArrayList<Token> tokens;
    private final ArrayList<Error> errors;
    private final HashMap<String, TokenType> keywords = new HashMap<String, TokenType>() {{
        put("main", TokenType.MAINTK);
        put("const", TokenType.CONSTTK);
        put("int", TokenType.INTTK);
        put("char", TokenType.CHARTK);
        put("break", TokenType.BREAKTK);
        put("continue", TokenType.CONTINUETK);
        put("if", TokenType.IFTK);
        put("else", TokenType.ELSETK);
        put("for", TokenType.FORTK);
        put("getint", TokenType.GETINTTK);
        put("getchar", TokenType.GETCHARTK);
        put("printf", TokenType.PRINTFTK);
        put("return", TokenType.RETURNTK);
        put("void", TokenType.VOIDTK);
    }};

    public Lexer(String programCode) {
        tokens = new ArrayList<>();
        errors = new ArrayList<>();
        tokenIndex = 0;
        lineno = 1;
        this.sourceCode = programCode;
    }

    public void analyzeCode() {
        Token token;
        while ((token = next()) != null) {
            if (token.getType() != TokenType.ANNOTATION) {
                tokens.add(token);
            }
        }
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public ArrayList<Error> getErrors() {
        return errors;
    }

    public Token next() {
        StringBuilder sb = new StringBuilder();
        // 去掉多余空白符
        while (tokenIndex < sourceCode.length() && (isSpace() || isTab() || isNewLine())) {
            tokenIndex++;
        }
        if (tokenIndex >= sourceCode.length()) {
            return null;
        }
        // 标识符 or 关键字
        if (isIdentNonDigit()) {
            while (isIdentNonDigit() || isDigit()) {
                sb.append(sourceCode.charAt(tokenIndex));
                tokenIndex++;
            }
            if (keywords.containsKey(sb.toString())) {
                return new Token(keywords.get(sb.toString()), sb.toString(), lineno);
            }
            return new Token(TokenType.IDENFR, sb.toString(), lineno);
        }
        // 数值常量
        else if (isDigit()) {
            while (isDigit()) {
                sb.append(sourceCode.charAt(tokenIndex));
                tokenIndex++;
            }
            return new Token(TokenType.INTCON, sb.toString(), lineno);
        }
        // 字符常量
        else if (sourceCode.charAt(tokenIndex) == '\'') {
            sb.append(sourceCode.charAt(tokenIndex));
            tokenIndex++;
            if (isAscii()) {
                sb.append(sourceCode.charAt(tokenIndex));
                if (isEscape()) {
                    tokenIndex++;
                    sb.append(sourceCode.charAt(tokenIndex));
                }
                tokenIndex++;
            }
            if (sourceCode.charAt(tokenIndex) == '\'') {
                sb.append(sourceCode.charAt(tokenIndex));
                tokenIndex++;
            }
            return new Token(TokenType.CHRCON, sb.toString(), lineno);
        }
        // 字符串常量
        else if (sourceCode.charAt(tokenIndex) == '\"') {
            sb.append(sourceCode.charAt(tokenIndex));
            tokenIndex++;
            while (isAscii() && sourceCode.charAt(tokenIndex) != '\"') {
                sb.append(sourceCode.charAt(tokenIndex));
                tokenIndex++;
            }
            if (sourceCode.charAt(tokenIndex) == '\"') {
                sb.append(sourceCode.charAt(tokenIndex));
                tokenIndex++;
            }
            return new Token(TokenType.STRCON, sb.toString(), lineno);
        }
        // NOT or NEQ
        else if (sourceCode.charAt(tokenIndex) == '!') {
            tokenIndex++;
            if (sourceCode.charAt(tokenIndex) == '=') {
                tokenIndex++;
                return new Token(TokenType.NEQ, "!=", lineno);
            }
            return new Token(TokenType.NOT, "!", lineno);
        }
        // AND
        else if (sourceCode.charAt(tokenIndex) == '&') {
            tokenIndex++;
            if (sourceCode.charAt(tokenIndex) == '&') {
                tokenIndex++;
                return new Token(TokenType.AND, "&&", lineno);
            } else {
                //TODO: else errors
                errors.add(new Error("a", lineno));
                return new Token(TokenType.AND, "&", lineno);
            }
        }
        // OR
        else if (sourceCode.charAt(tokenIndex) == '|') {
            tokenIndex++;
            if (sourceCode.charAt(tokenIndex) == '|') {
                tokenIndex++;
                return new Token(TokenType.OR, "||", lineno);
            } else {
                //TODO: else errors
                errors.add(new Error("a", lineno));
                return new Token(TokenType.OR, "|", lineno);
            }
        }
        // PLUS
        else if (sourceCode.charAt(tokenIndex) == '+') {
            tokenIndex++;
            return new Token(TokenType.PLUS, "+", lineno);
        }
        // MINU
        else if (sourceCode.charAt(tokenIndex) == '-') {
            tokenIndex++;
            return new Token(TokenType.MINU, "-", lineno);
        }
        // MULT
        else if (sourceCode.charAt(tokenIndex) == '*') {
            tokenIndex++;
            return new Token(TokenType.MULT, "*", lineno);
        }
        // DIV or Annotation
        else if (sourceCode.charAt(tokenIndex) == '/') {
            tokenIndex++;
            if (sourceCode.charAt(tokenIndex) == '/') {
                tokenIndex++;
                while ((sourceCode.charAt(tokenIndex) != '\n')) {
                    tokenIndex++;
                }
                return new Token(TokenType.ANNOTATION, "//", lineno);
            } else if (sourceCode.charAt(tokenIndex) == '*') {
                tokenIndex++;
                while ((sourceCode.charAt(tokenIndex) != '*')
                        || (sourceCode.charAt(tokenIndex + 1) != '/')) {
                    if (sourceCode.charAt(tokenIndex) == '\n') {
                        lineno++;
                    }
                    tokenIndex++;
                }
                tokenIndex += 2;
                return new Token(TokenType.ANNOTATION, "/**/", lineno);
            }
            return new Token(TokenType.DIV, "/", lineno);
        }
        // MOD
        else if (sourceCode.charAt(tokenIndex) == '%') {
            tokenIndex++;
            return new Token(TokenType.MOD, "%", lineno);
        }
        // LEQ or LSS
        else if (sourceCode.charAt(tokenIndex) == '<') {
            tokenIndex++;
            if (sourceCode.charAt(tokenIndex) == '=') {
                tokenIndex++;
                return new Token(TokenType.LEQ, "<=", lineno);
            }
            return new Token(TokenType.LSS, "<", lineno);
        }
        // GEQ or GRE
        else if (sourceCode.charAt(tokenIndex) == '>') {
            tokenIndex++;
            if (sourceCode.charAt(tokenIndex) == '=') {
                tokenIndex++;
                return new Token(TokenType.GEQ, ">=", lineno);
            }
            return new Token(TokenType.GRE, ">", lineno);
        }
        // EQL or ASSIGN
        else if (sourceCode.charAt(tokenIndex) == '=') {
            tokenIndex++;
            if (sourceCode.charAt(tokenIndex) == '=') {
                tokenIndex++;
                return new Token(TokenType.EQL, "==", lineno);
            }
            return new Token(TokenType.ASSIGN, "=", lineno);
        }
        // SEMICN
        else if (sourceCode.charAt(tokenIndex) == ';') {
            tokenIndex++;
            return new Token(TokenType.SEMICN, ";", lineno);
        }
        // COMMA
        else if (sourceCode.charAt(tokenIndex) == ',') {
            tokenIndex++;
            return new Token(TokenType.COMMA, ",", lineno);
        }
        // LPARENT
        else if (sourceCode.charAt(tokenIndex) == '(') {
            tokenIndex++;
            return new Token(TokenType.LPARENT, "(", lineno);
        }
        // RPARENT
        else if (sourceCode.charAt(tokenIndex) == ')') {
            tokenIndex++;
            return new Token(TokenType.RPARENT, ")", lineno);
        }
        // LBRACK
        else if (sourceCode.charAt(tokenIndex) == '[') {
            tokenIndex++;
            return new Token(TokenType.LBRACK, "[", lineno);
        }
        // RBRACK
        else if (sourceCode.charAt(tokenIndex) == ']') {
            tokenIndex++;
            return new Token(TokenType.RBRACK, "]", lineno);
        }
        // LBRACE
        else if (sourceCode.charAt(tokenIndex) == '{') {
            tokenIndex++;
            return new Token(TokenType.LBRACE, "{", lineno);
        }
        // RBRACE
        else if (sourceCode.charAt(tokenIndex) == '}') {
            tokenIndex++;
            return new Token(TokenType.RBRACE, "}", lineno);
        }
        return null;
    }

    public boolean isSpace() {
        return sourceCode.charAt(tokenIndex) == ' ';
    }

    public boolean isTab() {
        return sourceCode.charAt(tokenIndex) == '\t';
    }

    public boolean isNewLine() {
        if (sourceCode.charAt(tokenIndex) == '\n') {
            lineno++;
            return true;
        }
        return false;
    }

    public boolean isDigit() {
        return sourceCode.charAt(tokenIndex) >= '0' && sourceCode.charAt(tokenIndex) <= '9';
    }

    public boolean isIdentNonDigit() {
        char chr = sourceCode.charAt(tokenIndex);
        return Character.isLowerCase(chr) || Character.isUpperCase(chr) || chr == '_';
    }

    public boolean isAscii() {
        return sourceCode.charAt(tokenIndex) >= 32
                && sourceCode.charAt(tokenIndex) <= 126
                || isEscape();
    }

    public boolean isEscape() {
        return sourceCode.charAt(tokenIndex) == '\\' &&
                (sourceCode.charAt(tokenIndex + 1) == 'a'
                || sourceCode.charAt(tokenIndex + 1) == 'b'
                || sourceCode.charAt(tokenIndex + 1) == 't'
                || sourceCode.charAt(tokenIndex + 1) == 'n'
                || sourceCode.charAt(tokenIndex + 1) == 'v'
                || sourceCode.charAt(tokenIndex + 1) == 'f'
                || sourceCode.charAt(tokenIndex + 1) == '\"'
                || sourceCode.charAt(tokenIndex + 1) == '\''
                || sourceCode.charAt(tokenIndex + 1) == '\\'
                || sourceCode.charAt(tokenIndex + 1) == '0');
    }
}
