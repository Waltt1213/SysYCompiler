package table;

import frontend.Error;
import frontend.Token;
import frontend.TokenType;
import frontend.ast.AstNode;
import frontend.ast.Block;
import frontend.ast.CompUnit;
import frontend.ast.ConstDecl;
import frontend.ast.ConstDef;
import frontend.ast.ConstInitVal;
import frontend.ast.Exp;
import frontend.ast.ForStmt;
import frontend.ast.FuncDef;
import frontend.ast.FuncFParam;
import frontend.ast.FuncFParams;
import frontend.ast.FuncRParams;
import frontend.ast.InitVal;
import frontend.ast.LVal;
import frontend.ast.MainFuncDef;
import frontend.ast.Stmt;
import frontend.ast.UnaryExp;
import frontend.ast.VarDecl;
import frontend.ast.VarDef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class Visitor {
    private final CompUnit root;

    private final ArrayList<SymbolTable> symbolTables;    //  符号表集
    private final ArrayDeque<SymbolTable> tableStack;     // 符号表栈
    private final HashMap<AstNode, Symbol> nodeSymbolNap; // node-symbol对应表
    private final ArrayDeque<Symbol> symStack;            // 符号栈
    private final HashMap<String, Symbol> funcNameTable;
    private final ArrayDeque<BlockType> blockStack;
    private final SymbolTable globalTable;
    private SymbolTable curTable;
    private int curDepth;
    private Function curFunc;
    private final ArrayList<Error> errors;

    private enum BlockType {
        forBlock, FuncBlock
    }

    public Visitor(CompUnit compUnit, ArrayList<Error> errors) {
        this.root = compUnit;
        curDepth = 1;
        symbolTables = new ArrayList<>();
        tableStack = new ArrayDeque<>();
        symStack = new ArrayDeque<>();
        funcNameTable = new HashMap<>();
        blockStack = new ArrayDeque<>();
        nodeSymbolNap = new HashMap<>();
        globalTable = new SymbolTable(null, curDepth);
        this.errors = errors;
    }

    public void buildSymTable() {
        curTable = globalTable;
        tableStack.push(curTable);
        symbolTables.add(curTable);
        curFunc = null;
        visit(root, null);
    }

    public ArrayList<SymbolTable> getSymbolTables() {
        return symbolTables;
    }

    public HashMap<AstNode, Symbol> getNodeSymbolNap() {
        return nodeSymbolNap;
    }

    public HashMap<String, Symbol> getFuncNameTable() {
        return funcNameTable;
    }

    public ArrayList<Error> getErrors() {
        return errors;
    }

    public void visit(AstNode astNode, FuncFParams fps) {
        if (astNode == null) { return; }
        if (astNode instanceof Block) {
            visitBlock(fps);
        }
        if (astNode instanceof MainFuncDef) {
            visitMainFuncDef(((MainFuncDef) astNode));
        }
        if (astNode instanceof ConstDecl) {
            visitConstDecl((ConstDecl) astNode);
        } else if (astNode instanceof VarDecl) {
            visitVarDecl((VarDecl) astNode);
        } else if (astNode instanceof FuncDef) {
            visitFuncDef((FuncDef) astNode);
            return;
        }
        checkError(astNode);
        for (AstNode node : astNode.getAstChild()) {
            visit(node, null);
        }
        if (astNode instanceof Block) { // 此时Block内表建立完毕
            curDepth--;
            tableStack.pop();           // 符号表出栈
            symStack.removeAll(curTable.getSymItems().values());
            curTable = tableStack.peek();
        }
        if (astNode instanceof Stmt && ((Stmt) astNode).getType() == Stmt.StmtType.FOR) {
            blockStack.pop();
        }
    }

    public void visitFuncDef(FuncDef fd) {
        String funcType = getType(fd.getFuncType().getType());
        SymType ft = new SymType(funcType, false, false, true);
        String funcName = fd.getIdent().getContent();
        try {
            Function func = new Function(funcName, ft, curDepth, fd.getFuncType().getType().getLineno(), fd.getArgc());
            if (fd.hasFParams()) { func.setFuncFParams(fd.getFuncFParams()); }
            curFunc = func;
            symStack.push(func);
            curTable.addSymItem(funcName, func);
            funcNameTable.put(funcName, func);
            nodeSymbolNap.put(fd, func);
        } catch (Error e) {
            errors.add(new Error("b", fd.getFuncType().getType().getLineno()));
        }
        // 形参需要加入符号表，但属于下一层级
        blockStack.push(BlockType.FuncBlock);
        visit(fd.getBlock(), fd.getFuncFParams());  // 构建函数内符号表
        if (!ft.toString().equals("VoidFunc") && !fd.getBlock().hasRet()) {
            errors.add(new Error("g", fd.getBlock().getLineno()));
        }
    }

    public void visitBlock(FuncFParams fps) {
        curDepth++;
        curTable = new SymbolTable(curTable, curDepth);
        symbolTables.add(curTable); // 将符号表加入符号表集
        tableStack.push(curTable); // 入栈
        if (fps != null) {
            try {
                visitFuncFParams(fps);  // 函数形参与函数块内符号属于同一层级
            } catch (Error e) { errors.add(e); }
        }
    }

    public void visitConstDecl(ConstDecl cd) {
        String varType = getType(cd.getType()); // Int or Char
        for (AstNode astChild : cd.getAstChild()) {
            try {
                visitConstDef(varType, (ConstDef) astChild);
            } catch (Error e) {
                errors.add(new Error("b", ((ConstDef) astChild).getIdent().getLineno()));
            }
        }
    }

    public void visitMainFuncDef(MainFuncDef astNode) {
        SymType st = new SymType("Int", false, false, true);
        curFunc = new Function("main", st, curDepth, (astNode).getLineno(), 0);
        if (!(astNode).getBlock().hasRet()) {
            errors.add(new Error("g", (astNode).getBlock().getLineno()));
        }
        blockStack.push(BlockType.FuncBlock);
    }

    public void visitConstDef(String varType, ConstDef constDef) throws Error {
        String varName = constDef.getIdent().getContent();
        boolean isArray = constDef.hasArray();
        SymType st;
        if (isArray) {
            st = new SymType(varType, true, true, false); //TODO
        } else {
            st = new SymType(varType, false, true, false);
        }
        ConstInitVal civ = constDef.getConstInitVal();
        Variable var = new Variable(varName, st, curDepth, constDef.getIdent().getLineno(), civ);
        curTable.addSymItem(varName, var);
        symStack.push(var);
        nodeSymbolNap.put(constDef, var);
    }

    public void visitVarDecl(VarDecl vd) {
        String vaType = getType(vd.getType());
        for (AstNode node: vd.getAstChild()) {
            try {
                visitVarDef(vaType, (VarDef) node);
            } catch (Error e) {
                errors.add(new Error("b", ((VarDef) node).getIdent().getLineno()));
            }
        }
    }

    public void visitVarDef(String varType, VarDef varDef) throws Error {
        String varName = varDef.getIdent().getContent();
        boolean isArray = varDef.hasConstExp();
        SymType st;
        if (isArray) {
            st = new SymType(varType, true,false, false); // TODO
        } else {
            st = new SymType(varType, false, false, false);
        }
        InitVal iv = varDef.getInitVal();
        Variable var = new Variable(varName, st, curDepth, varDef.getIdent().getLineno(), iv);
        curTable.addSymItem(varName, var);
        symStack.push(var);
        nodeSymbolNap.put(varDef, var);
    }

    public void visitFuncFParams(FuncFParams fps) throws Error {
        for (AstNode node : fps.getAstChild()) {
            FuncFParam fp = (FuncFParam) node;
            String funcVarName = fp.getIdentName();
            String funcVarType = getType(fp.getType());
            SymType st;
            if (fp.isArray()) {
                st = new SymType(funcVarType, true, false, false);
            } else {
                st = new SymType(funcVarType, false, false, false);
            }
            FuncVar symbol = new FuncVar(funcVarName, st, curDepth, fp.getIdent().getLineno(), fps.getFuncName());
            curTable.addSymItem(funcVarName, symbol);
            symStack.push(symbol);
            nodeSymbolNap.put(fp, symbol);
        }
    }

    public boolean findSymInStack(String name, String type) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        ArrayList<Symbol> symbols = new ArrayList<>(symStack);
        for (Symbol sym : symbols) {
            if (sym.getName().equals(name) && sym.getVarOrFunc().equals(type)) {
                return true;
            }
        }
        return false;
    }

    public Symbol getSymInStack(String name, String type) {
        ArrayList<Symbol> symbols = new ArrayList<>(symStack);
        for (Symbol sym : symbols) {
            if (sym.getName().equals(name) && sym.getVarOrFunc().equals(type)) {
                return sym;
            }
        }
        return null;
    }

    public void checkFuncParams(FuncFParams ffp, FuncRParams frp) {
        if (ffp.getArgc() != frp.getArgc()) {
            return;
        }
        for (int i = 0; i < ffp.getArgc(); i++) {
            FuncFParam fp = ffp.getParam(i);
            Exp rp = frp.getParam(i);
            boolean isArray = false;    //  是否为数组
            String type = "";        //  类型为what, 只考虑数组情况。
            String name = rp.getIdentName();    //获取符号
            if (findSymInStack(name, "Var")) {
                Symbol sym;
                sym = getSymInStack(name, "Var");
                SymType st = sym.getType();
                isArray = sym.getType().isArray() && !rp.isArray();
                type = st.getType().toLowerCase();
            }
            if (isArray != fp.isArray()) {  // array or var不匹配
                errors.add(new Error("e", rp.getLineno()));
                return;
            } else if (isArray && fp.isArray()) {    // char or int 不匹配
                if (!type.equals(fp.getType().getContent())) {
                    errors.add(new Error("e", rp.getLineno()));
                    return;
                }
            }
        }
    }

    public String getType(Token type) {
        if (type.getType() == TokenType.INTTK) {
            return "Int";
        } else if (type.getType() == TokenType.CHARTK) {
            return "Char";
        } else if (type.getType() == TokenType.VOIDTK) {
            return "Void";
        } else {
            return null;
        }
    }

    public void checkError(AstNode astNode) {
        if (astNode instanceof LVal) {
            String name = ((LVal) astNode).getIdent().getContent();
            if (!findSymInStack(name, "Var")) {
                errors.add(new Error("c", ((LVal) astNode).getIdent().getLineno()));
            }
        } else if (astNode instanceof UnaryExp && ((UnaryExp)astNode).isIdent()) {
            String name = ((UnaryExp) astNode).getIdent().getContent();
            if (!findSymInStack(name, "Func")) {
                errors.add(new Error("c", ((UnaryExp) astNode).getIdent().getLineno()));
            }
            if (((UnaryExp) astNode).hasFuncRParams() && funcNameTable.containsKey(name)) {
                UnaryExp ue = (UnaryExp) astNode;
                Function func = (Function) funcNameTable.get(name);
                if (func != null && ue.getArgc() != func.getArgc()) {
                    errors.add(new Error("d", ue.getLineno()));
                } else if (func != null) {
                    checkFuncParams(func.getFuncFParams(), ue.getFuncRParams());
                }
            }
        } else if (astNode instanceof Stmt && ((Stmt) astNode).getType() == Stmt.StmtType.RETURN) {
            if (curFunc.getType().toString().equals("VoidFunc") && !astNode.getAstChild().isEmpty()) {
                errors.add(new Error("f", ((Stmt) astNode).getLineno()));
            }
        } else if (astNode instanceof Stmt && ((Stmt) astNode).getType() == Stmt.StmtType.FOR) {
            blockStack.push(BlockType.forBlock);
        } else if (astNode instanceof Stmt && (((Stmt) astNode).getType() == Stmt.StmtType.BREAK
                || ((Stmt) astNode).getType() == Stmt.StmtType.CONTINUE)) {
            BlockType bt = blockStack.peek();
            if (bt != BlockType.forBlock) {
                errors.add(new Error("m", ((Stmt) astNode).getLineno()));
            }
        } else if (astNode instanceof Stmt && ((Stmt) astNode).getType() == Stmt.StmtType.PRINTF) {
            Stmt printf = (Stmt) astNode;
            if (printf.FormatNum() != printf.getAstChild().size()) {
                errors.add(new Error("l", printf.getLineno()));
            }
        } else if (astNode instanceof Stmt && !astNode.getAstChild().isEmpty() && astNode.getAstChild().get(0) instanceof LVal) {
            String name = ((LVal) astNode.getAstChild().get(0)).getIdentName();
            if (findSymInStack(name, "Var")) {
                Symbol sym = getSymInStack(name, "Var");
                if (sym.getType().isConst()) {
                    if (((Stmt) astNode).iaNormalAssign()) {
                        errors.add(new Error("h", ((Stmt) astNode).getLineno()));
                    }
                }
            }
        } else if (astNode instanceof ForStmt) {
            String name = ((LVal) astNode.getAstChild().get(0)).getIdentName();
            if (findSymInStack(name, "Var")) {
                Symbol sym = getSymInStack(name, "Var");
                if (sym.getType().isConst()) {
                    errors.add(new Error("h", ((ForStmt) astNode).getLineno()));
                }
            }
        }
    }
}
