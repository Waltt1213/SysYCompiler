package frontend;

import frontend.ast.*;
import frontend.ast.Character;
import frontend.ast.Number;

import java.util.ArrayList;
import java.util.Comparator;

public class Parser {
    private final ArrayList<Token> lexerCode;
    private final ArrayList<Error> errors;
    private int lexerIndex;
    private CompUnit compUnit;
    private String curFuncName;

    public Parser(ArrayList<Token> lexerCode, ArrayList<Error> errors) {
        this.lexerCode = lexerCode;
        lexerIndex = 0;
        compUnit = null;
        this.errors = errors;
    }

    public void analyzeTokens() {
        lexerIndex = 0;
        compUnit = parseCompUnit();
    }

    public CompUnit getCompUnit() {
        return compUnit;
    }

    public Token sym() {
        return lexerCode.get(lexerIndex);
    }

    public Token sym(int bis) {
        return lexerCode.get(lexerIndex + bis);
    }

    public void nextSym() {
        lexerIndex++;
    }

    public void nextSym(int bis) {
        lexerIndex += bis;
    }

    public CompUnit parseCompUnit() {
        ArrayList<AstNode> decls = new ArrayList<>();
        ArrayList<FuncDef> funcDefs = new ArrayList<>();
        MainFuncDef mainFuncDef = null;
        try {
            // MainFuncDef
            if (sym(1).getType() == TokenType.MAINTK) {
                curFuncName = "main";
                mainFuncDef = parseMainFuncDef();
                return new CompUnit(decls, funcDefs, mainFuncDef);
            } else { // Decl
                while (sym(2).getType() != TokenType.LPARENT) {
                    if (sym().getType() == TokenType.CONSTTK) {
                        decls.add(parseConstDecl());
                    } else {
                        decls.add(parseVarDecl());
                    }
                }
            }
            // Main
            if (sym(1).getType() == TokenType.MAINTK) {
                curFuncName = "main";
                mainFuncDef = parseMainFuncDef();
                return new CompUnit(decls, funcDefs, mainFuncDef);
            } else { // FuncDef
                while (sym(2).getType() == TokenType.LPARENT
                        && sym(1).getType() != TokenType.MAINTK) {
                    funcDefs.add(parseFuncDef());
                }
            }
            curFuncName = "main";
            mainFuncDef = parseMainFuncDef();
        } catch (Error error) {
            errors.sort(Comparator.comparing(Error::getLineno));
        }
        return new CompUnit(decls, funcDefs, mainFuncDef);
    }

    public MainFuncDef parseMainFuncDef() throws Error {
        int lineno = sym().getLineno();
        nextSym(3);  // int main (
        if (sym().getType() == TokenType.RPARENT) {
            nextSym();
        } else {
            errors.add(new Error("j", sym(-1).getLineno()));
        }
        return new MainFuncDef(parseBlock(), lineno);
    }

    public ConstDecl parseConstDecl() throws Error {
        Token type;
        nextSym(); // const
        type = sym();
        nextSym();
        ArrayList<ConstDef> constDefs = new ArrayList<>();
        constDefs.add(parseConstDef());
        while (sym().getType() == TokenType.COMMA) {
            nextSym();
            constDefs.add(parseConstDef());
        }
        if (sym().getType() == TokenType.SEMICN) {
            nextSym();
        } else {
            errors.add(new Error("i", sym(-1).getLineno()));
        }
        return new ConstDecl(type, constDefs);
    }

    public VarDecl parseVarDecl() throws Error {
        Token type;
        type = sym();
        nextSym();
        ArrayList<VarDef> varDefs = new ArrayList<>();
        varDefs.add(parseVarDef());
        while (sym().getType() == TokenType.COMMA) {
            nextSym();
            varDefs.add(parseVarDef());
        }
        if (sym().getType() == TokenType.SEMICN) {
            nextSym();
        } else {
            errors.add(new Error("i", sym(-1).getLineno()));
        }
        return new VarDecl(type, varDefs);
    }

    public FuncDef parseFuncDef() throws Error {
        FuncType funcType = new FuncType(sym());
        nextSym();
        if (sym().getType() != TokenType.IDENFR) {
            throw new Error("myError", sym().getLineno());
        }
        Token ident = sym();
        curFuncName = ident.getContent();
        nextSym();
        nextSym(); // 跳过左括号
        FuncFParams funcFParams;
        if (sym().getType() != TokenType.RPARENT && isBType(sym())) { // 有params
            funcFParams = parseFuncFParams();
            if (sym().getType() == TokenType.RPARENT) {
                nextSym(); // 跳过右括号
            } else {
                errors.add(new Error("j", sym(-1).getLineno()));
            }
            return new FuncDef(funcType, ident, funcFParams, parseBlock());
        }
        if (sym().getType() == TokenType.RPARENT) {
            nextSym(); // 跳过右括号
        } else {
            errors.add(new Error("j", sym(-1).getLineno()));
        }
        return new FuncDef(funcType, ident, parseBlock());
    }

    public ConstDef parseConstDef() throws Error {
        if (sym().getType() != TokenType.IDENFR) {
            throw new Error("myError", sym().getLineno());
        }
        Token ident = sym();
        nextSym();
        // 数组
        if (sym().getType() == TokenType.LBRACK) {
            nextSym();
            ConstExp constExp = new ConstExp(parseAddExp());
            if (sym().getType() == TokenType.RBRACK) {
                nextSym(); // 跳过]
            } else {
                errors.add(new Error("k", sym(-1).getLineno()));
            }
            nextSym(); // 跳过=
            return new ConstDef(ident, constExp, parseConstInitVal());
        }
        nextSym();  // =
        return new ConstDef(ident, parseConstInitVal());
    }

    public VarDef parseVarDef() throws Error {
        if (sym().getType() != TokenType.IDENFR) {
            throw new Error("myError", sym().getLineno());
        }
        Token ident = sym();
        nextSym();
        if (sym().getType() == TokenType.LBRACK) {
            nextSym();
            ConstExp constExp = new ConstExp(parseAddExp());
            if (sym().getType() == TokenType.RBRACK) {
                nextSym(); // 跳过]
            } else {
                errors.add(new Error("k", sym(-1).getLineno()));
            }
            if (sym().getType() == TokenType.ASSIGN) {
                nextSym();
                return new VarDef(ident, constExp, parseInitVal());
            }
            return new VarDef(ident, constExp);
        } else {
            if (sym().getType() == TokenType.ASSIGN) {
                nextSym();
                return new VarDef(ident, parseInitVal());
            }
            return new VarDef(ident);
        }
    }

    public FuncFParams parseFuncFParams() {
        ArrayList<FuncFParam> funcFParams = new ArrayList<>();
        funcFParams.add(parseFuncFParam());
        while (sym().getType() == TokenType.COMMA) {
            nextSym();
            funcFParams.add(parseFuncFParam());
        }
        return new FuncFParams(funcFParams, curFuncName);
    }

    public FuncFParam parseFuncFParam() {
        Token type = sym();
        nextSym();
        Token ident = sym();
        nextSym();
        if (sym().getType() == TokenType.LBRACK) {
            nextSym();
            if (sym().getType() == TokenType.RBRACK) {
                nextSym();
            } else {
                errors.add(new Error("k", sym(-1).getLineno()));
            }
            return new FuncFParam(type, ident, true);
        }
        return new FuncFParam(type, ident, false);
    }

    public FuncRParams parseFuncRParams() throws Error {
        ArrayList<Exp> exps = new ArrayList<>();
        exps.add(new Exp(parseAddExp()));
        while (sym().getType() == TokenType.COMMA) {
            nextSym();
            exps.add(new Exp(parseAddExp()));
        }
        return new FuncRParams(exps);
    }

    public Block parseBlock() throws Error {
        ArrayList<AstNode> blockItems = new ArrayList<>();
        nextSym(); // 跳过{
        boolean hasRet = false;
        while (sym().getType() != TokenType.RBRACE) {
            AstNode astNode = parseBlockItem();
            blockItems.add(astNode);
            if (astNode instanceof Stmt && ((Stmt) astNode).getType() == Stmt.StmtType.RETURN) {
                hasRet = true;
            }
        }
        int lineno = sym().getLineno();
        nextSym(); // 跳过}
        return new Block(blockItems, hasRet, lineno);
    }

    public AstNode parseBlockItem() throws Error {
        if (sym().getType() == TokenType.CONSTTK) {
            return parseConstDecl();
        } else if (isBType(sym())) {
            return parseVarDecl();
        } else {
            return parseStmt();
        }
    }

    public ConstInitVal parseConstInitVal() throws Error {
        if (sym().getType() == TokenType.LBRACE) {
            nextSym();
            ArrayList<ConstExp> constExps = new ArrayList<>();
            while (sym().getType() != TokenType.RBRACE) {
                constExps.add(new ConstExp(parseAddExp()));
                if (sym().getType() == TokenType.COMMA) {
                    nextSym();
                }
            }
            if (sym().getType() == TokenType.RBRACE) {
                nextSym();
            }
            return new ConstInitVal(constExps);
        } else if (sym().getType() == TokenType.STRCON) {
            Token stringConst = sym();
            nextSym();
            return new ConstInitVal(stringConst);
        } else {
            return new ConstInitVal(new ConstExp(parseAddExp()));
        }
    }

    public InitVal parseInitVal() throws Error {
        if (sym().getType() == TokenType.LBRACE) {
            nextSym();
            ArrayList<Exp> exps = new ArrayList<>();
            while (sym().getType() != TokenType.RBRACE) {
                exps.add(new Exp(parseAddExp()));
                if (sym().getType() == TokenType.COMMA) {
                    nextSym();
                }
            }
            if (sym().getType() == TokenType.RBRACE) {
                nextSym();
            }
            return new InitVal(exps);
        } else if (sym().getType() == TokenType.STRCON) {
            Token stringConst = sym();
            nextSym();
            return new InitVal(stringConst);
        } else {
            return new InitVal(new Exp(parseAddExp()));
        }
    }

    public Stmt parseStmt() throws Error {
        Stmt.StmtType stmtType;
        ArrayList<AstNode> stmts = new ArrayList<>();
        int lineno = sym().getLineno();
        if (sym().getType() == TokenType.IFTK) {
            stmtType = Stmt.StmtType.IF;
            nextSym(2); // 跳过 if (
            stmts.add(new Cond(parseLOrExp())); // cond
            if (sym().getType() == TokenType.RPARENT) {
                nextSym(); // )
            } else {
                errors.add(new Error("j", sym(-1).getLineno()));
            }
            stmts.add(parseStmt()); // stmt
            if (sym().getType() == TokenType.ELSETK) {
                nextSym();
                stmts.add(parseStmt());
            }
        } else if (sym().getType() == TokenType.FORTK) {
            stmtType = Stmt.StmtType.FOR;
            nextSym(2); // for (
            if (sym().getType() != TokenType.SEMICN) {
                stmts.add(parseForStmt());
            } else {
                stmts.add(null);
            }
            nextSym(); // ;
            if (sym().getType() != TokenType.SEMICN) {
                stmts.add(new Cond(parseLOrExp()));
            } else {
                stmts.add(null);
            }
            nextSym(); // ;
            if (sym().getType() != TokenType.RPARENT) {
                stmts.add(parseForStmt());
            } else {
                stmts.add(null);
            }
            nextSym(); // )
            stmts.add(parseStmt());
        } else if (sym().getType() == TokenType.BREAKTK) {
            stmtType = Stmt.StmtType.BREAK;
            nextSym();
            if (sym().getType() == TokenType.SEMICN) {
                nextSym();
            } else {
                errors.add(new Error("i", sym(-1).getLineno()));
            }
        } else if (sym().getType() == TokenType.CONTINUETK) {
            stmtType = Stmt.StmtType.CONTINUE;
            nextSym();
            if (sym().getType() == TokenType.SEMICN) {
                nextSym();
            } else {
                errors.add(new Error("i", sym(-1).getLineno()));
            }
        } else if (sym().getType() == TokenType.RETURNTK) {
            stmtType = Stmt.StmtType.RETURN;
            nextSym(); // return
            int preLexerIndex = lexerIndex;
            if (sym().getType() != TokenType.SEMICN) {
                try {
                    stmts.add(new Exp(parseAddExp()));
                } catch (Error e) {
                    lexerIndex = preLexerIndex;
                }
            }
            if (sym().getType() == TokenType.SEMICN) {
                nextSym();
            } else {
                errors.add(new Error("i", sym(-1).getLineno()));
            }
        } else if (sym().getType() == TokenType.PRINTFTK) {
            stmtType = Stmt.StmtType.PRINTF;
            nextSym(2); // printf (
            Token stringConst = sym();
            nextSym();
            while (sym().getType() == TokenType.COMMA) {
                nextSym(); // ,
                stmts.add(new Exp(parseAddExp()));
            }
            if (sym().getType() == TokenType.RPARENT) {
                nextSym(); // )
            } else  {
                errors.add(new Error("j", sym(-1).getLineno()));
            }
            if (sym().getType() == TokenType.SEMICN) {
                nextSym(); // ;
            } else {
                errors.add(new Error("i", sym(-1).getLineno()));
            }
            return new Stmt(stmts, stringConst, stmtType);
        } else if (sym().getType() == TokenType.SEMICN) {
            stmtType = Stmt.StmtType.NONE;
            nextSym();
        } else if (sym().getType() == TokenType.LBRACE) {
            stmtType = Stmt.StmtType.BLOCK;
            stmts.add(parseBlock());
        } else {
            int preLexerIndex = lexerIndex;
            boolean isExp = false;
            LVal lVal = null;
            try {
                lVal = parseLVal();
                if (sym().getType() != TokenType.ASSIGN) {
                    lexerIndex = preLexerIndex;
                    isExp = true;
                }
            } catch (Error ignored) {
                lexerIndex = preLexerIndex;
                isExp = true; // 可能是[Exp] ;或其i类错误
            }
            if (isExp) {
                stmtType = Stmt.StmtType.EXP;
                try {
                    stmts.add(new Exp(parseAddExp()));
                } catch (Error e) {
                    lexerIndex = preLexerIndex;
                }
            } else {
                stmts.add(lVal);
                nextSym(); // =
                if (sym().getType() == TokenType.GETINTTK) {
                    stmtType = Stmt.StmtType.GETINT;
                    nextSym(2); // getint(
                    if (sym().getType() == TokenType.RPARENT) {
                        nextSym(); // )
                    } else {
                        errors.add(new Error("j", sym(-1).getLineno()));
                    }
                } else if (sym().getType() == TokenType.GETCHARTK) {
                    stmtType = Stmt.StmtType.GETCHAR;
                    nextSym(2); // getchar(
                    if (sym().getType() == TokenType.RPARENT) {
                        nextSym(); // )
                    } else {
                        errors.add(new Error("j", sym(-1).getLineno()));
                    }
                } else {
                    stmtType = Stmt.StmtType.ASSIGN;
                    stmts.add(new Exp(parseAddExp()));
                }
            }
            if (sym().getType() == TokenType.SEMICN) {
                nextSym(); // ;
            } else {
                errors.add(new Error("i", sym(-1).getLineno()));
            }
        }
        return new Stmt(stmts, stmtType, lineno);
    }

    public ForStmt parseForStmt() throws Error {
        LVal lVal = parseLVal();
        if (sym().getType() == TokenType.ASSIGN) {
            nextSym();
        }
        Exp exp = new Exp(parseAddExp());
        return new ForStmt(lVal, exp);
    }

    public AddExp parseAddExp() throws Error {
        ArrayList<MulExp> mulExps = new ArrayList<>();
        ArrayList<Token> ops = new ArrayList<>();
        mulExps.add(parseMulExp());
        while (sym().getType() == TokenType.PLUS || sym().getType() == TokenType.MINU) {
            ops.add(sym());
            nextSym();
            mulExps.add(parseMulExp());
        }
        return new AddExp(mulExps, ops);
    }

    public MulExp parseMulExp() throws Error {
        ArrayList<UnaryExp> unaryExps = new ArrayList<>();
        ArrayList<Token> ops = new ArrayList<>();
        unaryExps.add(parseUnaryExp());
        while (sym().getType() == TokenType.MULT
                || sym().getType() == TokenType.DIV
                || sym().getType() == TokenType.MOD) {
            ops.add(sym());
            nextSym();
            unaryExps.add(parseUnaryExp());
        }
        return new MulExp(unaryExps, ops);
    }

    public UnaryExp parseUnaryExp() throws Error {
        if (isUnaryOp(sym())) {
            UnaryOp unaryOp = new UnaryOp(sym());
            nextSym();
            return new UnaryExp(unaryOp, parseUnaryExp());
        } else if (sym().getType() == TokenType.IDENFR && sym(1).getType() == TokenType.LPARENT) {
            Token ident = sym();
            nextSym(2); // 跳过 ident (
            FuncRParams funcRParams = null;
            int preLexerIndex = lexerIndex;
            try {
                funcRParams = parseFuncRParams();
            } catch (Error e) {
                lexerIndex = preLexerIndex;
            }
            if (sym().getType() == TokenType.RPARENT) {
                nextSym(); //跳过)
            } else {
                errors.add(new Error("j", sym(-1).getLineno()));
            }
            if (funcRParams == null) {
                return new UnaryExp(ident);
            }
            return new UnaryExp(ident, funcRParams);
        } else {
            return new UnaryExp(parsePrimaryExp());
        }
    }

    public PrimaryExp parsePrimaryExp() throws Error {
        if (sym().getType() == TokenType.LPARENT) {
            nextSym();
            Exp exp = new Exp(parseAddExp());
            if (sym().getType() == TokenType.RPARENT) {
                nextSym();
            } else {
                errors.add(new Error("j", sym(-1).getLineno()));
            }
            return new PrimaryExp(exp);
        } else if (sym().getType() == TokenType.INTCON) {
            Token number = sym();
            nextSym();
            return new PrimaryExp(new Number(number));
        } else if (sym().getType() == TokenType.CHRCON) {
            Token charConst = sym();
            nextSym();
            return new PrimaryExp(new Character(charConst));
        } else {
            return new PrimaryExp(parseLVal());
        }
    }

    public LVal parseLVal() throws Error {
        if (sym().getType() != TokenType.IDENFR) {
            throw new Error("myError", sym().getLineno());
        }
        Token ident = sym();
        nextSym();
        if (sym().getType() == TokenType.LBRACK) {
            nextSym();
            Exp exp = new Exp(parseAddExp());
            if (sym().getType() == TokenType.RBRACK) {
                nextSym();
            } else {
                errors.add(new Error("k", sym(-1).getLineno()));
            }
            return new LVal(ident, exp);
        } else {
            return new LVal(ident);
        }
    }

    public LOrExp parseLOrExp() throws Error {
        ArrayList<LAndExp> lAndExps = new ArrayList<>();
        lAndExps.add(parseLAndExp());
        while (sym().getType() == TokenType.OR) {
            nextSym();
            lAndExps.add(parseLAndExp());
        }
        return new LOrExp(lAndExps);
    }

    public LAndExp parseLAndExp() throws Error {
        ArrayList<EqExp> eqExps = new ArrayList<>();
        eqExps.add(parseEqExp());
        while (sym().getType() == TokenType.AND) {
            nextSym();
            eqExps.add(parseEqExp());
        }
        return new LAndExp(eqExps);
    }

    public EqExp parseEqExp() throws Error {
        ArrayList<RelExp> relExps = new ArrayList<>();
        relExps.add(parseRelExp());
        ArrayList<Token> ops = new ArrayList<>();
        while (sym().getType() == TokenType.EQL || sym().getType() == TokenType.NEQ) {
            ops.add(sym());
            nextSym();
            relExps.add(parseRelExp());
        }
        return new EqExp(relExps, ops);
    }

    public RelExp parseRelExp() throws Error {
        ArrayList<AddExp> addExps = new ArrayList<>();
        ArrayList<Token> ops = new ArrayList<>();
        addExps.add(parseAddExp());
        while (isRelOp(sym())) {
            ops.add(sym());
            nextSym();
            addExps.add(parseAddExp());
        }
        return new RelExp(addExps, ops);
    }

    public boolean isBType(Token type) {
        return type.getType() == TokenType.INTTK
                || type.getType() == TokenType.CHARTK;
    }

    public boolean isUnaryOp(Token op) {
        return op.getType() == TokenType.PLUS
                || op.getType() == TokenType.MINU
                || op.getType() == TokenType.NOT;
    }

    public boolean isRelOp(Token op) {
        return op.getType() == TokenType.GRE
                || op.getType() == TokenType.LSS
                || op.getType() == TokenType.GEQ
                || op.getType() == TokenType.LEQ;
    }

    public ArrayList<Error> getErrors() {
        return errors;
    }
}
