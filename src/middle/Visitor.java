package middle;

import frontend.Error;
import frontend.Token;
import frontend.TokenType;
import frontend.ast.*;
import llvmir.Module;
import middle.symbol.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

import static llvmir.TypeId.VoidTyID;

public class Visitor {
    private final CompUnit root;
    private final ArrayList<SymbolTable> symbolTables;    //  符号表集
    private final ArrayDeque<SymbolTable> tableStack;     // 符号表栈
    private final HashMap<Token, Symbol> nodeSymbolNap; // node-symbol对应表
    private final ArrayDeque<Symbol> symStack;            // 符号栈
    private final Module module = new Module(VoidTyID, "global");
    private final HashMap<String, Symbol> funcNameTable;
    private final ArrayDeque<BlockType> blockStack;
    private final SymbolTable globalTable;
    private SymbolTable curTable;
    private int curDepth;
    private FuncSym curFunc;
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

    public void buildIR() {
        curTable = globalTable;
        tableStack.push(curTable);
        symbolTables.add(curTable);
        curFunc = null;
        visitCompUnit(root);
        tableStack.clear();
        symStack.clear();
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

    public ArrayList<SymbolTable> getSymbolTables() {
        return symbolTables;
    }

    public HashMap<Token, Symbol> getNodeSymbolNap() {
        return nodeSymbolNap;
    }

    public HashMap<String, Symbol> getFuncNameTable() {
        return funcNameTable;
    }

    public ArrayList<Error> getErrors() {
        return errors;
    }

    public void visitCompUnit(CompUnit compUnit) {
        for (AstNode decl: compUnit.getDecls()) {
            if (decl instanceof ConstDecl) {
                visitConstDecl((ConstDecl) decl);
            } else {
                visitVarDecl((VarDecl) decl);
            }
        }
        for (FuncDef decl : compUnit.getFuncDefs()) {
            visitFuncDef(decl);
        }
        visitMainFuncDef(compUnit.getMainFuncDef());
    }

    public void visitFuncDef(FuncDef fd) {
        String funcType = getType(fd.getFuncType().getType());
        SymType ft = new SymType(funcType, false, false, true);
        String funcName = fd.getIdent().getContent();
        try {
            FuncSym func = new FuncSym(funcName, ft, curDepth, fd.getFuncType().getType().getLineno(), fd.getArgc());
            if (fd.hasFParams()) { func.setFuncFParams(fd.getFuncFParams()); }
            curFunc = func;
            symStack.push(func);
            curTable.addSymItem(funcName, func);
            funcNameTable.put(funcName, func);
            nodeSymbolNap.put(fd.getIdent(), func);
        } catch (Error e) {
            errors.add(new Error("b", fd.getFuncType().getType().getLineno()));
        }
        // 形参需要加入符号表，但属于下一层级
        blockStack.push(BlockType.FuncBlock);
        visitBlock(fd.getBlock(), fd.getFuncFParams());  // 构建函数内符号表
        if (!ft.toString().equals("VoidFunc") && !fd.getBlock().hasRet()) {
            errors.add(new Error("g", fd.getBlock().getLineno()));
        }
    }

    public void visitBlock(Block block, FuncFParams fps) {
        curDepth++;
        SymbolTable st = new SymbolTable(curTable, curDepth);
        curTable.addChildren(st);   // 新表设为当前表的孩子
        curTable = st;
        symbolTables.add(curTable); // 将符号表加入符号表集
        tableStack.push(curTable); // 入栈
        if (fps != null) {
            try {
                visitFuncFParams(fps);  // 函数形参与函数块内符号属于同一层级
            } catch (Error e) { errors.add(e); }
        }
        for (AstNode blockItem: block.getBlockItems()) {
            if (blockItem instanceof ConstDecl) {
                visitConstDecl((ConstDecl) blockItem);
            } else if (blockItem instanceof VarDecl) {
                visitVarDecl((VarDecl) blockItem);
            } else {
                visitStmt((Stmt) blockItem);
            }
        }
        curDepth--;
        tableStack.pop();           // 符号表出栈
        symStack.removeAll(curTable.getSymItems().values());
        curTable = tableStack.peek();
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
        curFunc = new FuncSym("main", st, curDepth, (astNode).getLineno(), 0);
        if (!astNode.getBlock().hasRet()) {
            errors.add(new Error("g", (astNode).getBlock().getLineno()));
        }
        blockStack.push(BlockType.FuncBlock);
        visitBlock(astNode.getBlock(), null);
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
        nodeSymbolNap.put(constDef.getIdent(), var);
        if (constDef.hasArray()) {
            visitConstExp(constDef.getConstExp());
        }
        visitConstInitVal(constDef.getConstInitVal());
    }

    public void visitConstExp(ConstExp constExp) {
        visitAddExp(constExp.getAddExp());
    }

    public void visitConstInitVal(ConstInitVal constInitVal) {
        if (constInitVal.isConstExp()) {
            visitConstExp(constInitVal.getConstExp());
        } else if (constInitVal.isConstExps()) {
            for (ConstExp constExp : constInitVal.getConstExps()) {
                visitConstExp(constExp);
            }
        }
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
        nodeSymbolNap.put(varDef.getIdent(), var);
        if (varDef.hasConstExp()) {
            visitConstExp(varDef.getConstExp());
        }
        if (varDef.hasInitVal()) {
            visitInitVal(varDef.getInitVal());
        }
    }

    public void visitInitVal(InitVal initVal) {
        if (initVal.isExp()) {
            visitAddExp(initVal.getExp().getAddExp());
        } else if (initVal.isExps()) {
            for (Exp exp: initVal.getExps()) {
                visitAddExp(exp.getAddExp());
            }
        }
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
            nodeSymbolNap.put(fp.getIdent(), symbol);
        }
    }

    public void visitStmt(Stmt stmt) {
        if (stmt.getType() == Stmt.StmtType.RETURN) {
            if (curFunc.getType().toString().equals("VoidFunc") && !stmt.getStmts().isEmpty()) {
                errors.add(new Error("f", stmt.getLineno()));
            }
            if (!stmt.getStmts().isEmpty()) {
                visitAddExp(((Exp) stmt.getStmts().get(0)).getAddExp());
            }
        } else if (stmt.getType() == Stmt.StmtType.IF) {
            visitLOrExp(((Cond) stmt.getStmts().get(0)).getLorExp());
            visitStmt((Stmt) stmt.getStmts().get(1));
            if (stmt.getStmts().size() > 2) {
                visitStmt((Stmt) stmt.getStmts().get(2));
            }
        } else if (stmt.getType() == Stmt.StmtType.FOR) {
            blockStack.push(BlockType.forBlock);
            for (AstNode node : stmt.getStmts()) {
                if (node instanceof ForStmt) {
                    visitForStmt((ForStmt) node);
                } else if (node instanceof Cond) {
                    visitLOrExp(((Cond) node).getLorExp());
                } else if (node instanceof Stmt) {
                    visitStmt((Stmt) node);
                }
            }
            blockStack.pop();
        } else if (stmt.getType() == Stmt.StmtType.BREAK
                || stmt.getType() == Stmt.StmtType.CONTINUE) {
            BlockType bt = blockStack.peek();
            if (bt != BlockType.forBlock) {
                errors.add(new Error("m", stmt.getLineno()));
            }
        } else if (stmt.getType() == Stmt.StmtType.PRINTF) {
            if (stmt.FormatNum() != stmt.getStmts().size()) {
                errors.add(new Error("l", stmt.getLineno()));
            }
            for (AstNode exp: stmt.getStmts()) {
                visitAddExp(((Exp) exp).getAddExp());
            }
        } else if (stmt.getType() == Stmt.StmtType.EXP) {
            visitAddExp(((Exp) stmt.getStmts().get(0)).getAddExp());
        } else if (stmt.getType() == Stmt.StmtType.BLOCK) {
            visitBlock((Block) stmt.getStmts().get(0), null);
        } else if (!stmt.getStmts().isEmpty() && stmt.getStmts().get(0) instanceof LVal) {
            String name = ((LVal) stmt.getStmts().get(0)).getIdentName();
            if (findSymInStack(name, "Var")) {
                Symbol sym = getSymInStack(name, "Var");
                if (sym.getType().isConst()) {
                    if (stmt.iaNormalAssign()) {
                        errors.add(new Error("h", stmt.getLineno()));
                    }
                }
            }
            visitLVal((LVal) stmt.getStmts().get(0));
            if (stmt.getType() == Stmt.StmtType.ASSIGN) {
                visitAddExp(((Exp) stmt.getStmts().get(1)).getAddExp());
            }
        }
    }

    public void visitForStmt(ForStmt forStmt) {
        String name = forStmt.getLval().getIdentName();
        if (findSymInStack(name, "Var")) {
            Symbol sym = getSymInStack(name, "Var");
            if (sym.getType().isConst()) {
                errors.add(new Error("h", forStmt.getLineno()));
            }
        }
        visitLVal(forStmt.getLval());
        visitAddExp(forStmt.getExp().getAddExp());
    }

    public void visitLOrExp(LOrExp lOrExp) {
        for (LAndExp lAndExp : lOrExp.getLAndExps()) {
            visitLAndExp(lAndExp);
        }
    }

    public void visitLAndExp(LAndExp lAndExp) {
        for (EqExp eqExp : lAndExp.getEqExps()) {
            visitEqExp(eqExp);
        }
    }

    public void visitEqExp(EqExp eqExp) {
        for (RelExp relExp: eqExp.getRelExps()) {
            visitRelExp(relExp);
        }
    }

    public void visitRelExp(RelExp relExp) {
        for (AddExp addExp: relExp.getAddExps()) {
            visitAddExp(addExp);
        }
    }

    public void visitAddExp(AddExp addExp) {
        for (MulExp mulExp : addExp.getMulExps()) {
            visitMulExp(mulExp);
        }
    }

    public void visitMulExp(MulExp mulExp) {
        for (UnaryExp unaryExp : mulExp.getUnaryExps()) {
            visitUnaryExp(unaryExp);
        }
    }

    public void visitUnaryExp(UnaryExp unaryExp) {
        if (unaryExp.isIdent()) {
            String name = unaryExp.getIdentName();
            if (!findSymInStack(name, "Func")) {
                errors.add(new Error("c", unaryExp.getIdent().getLineno()));
            }
            if (unaryExp.hasFuncRParams() && funcNameTable.containsKey(name)) {
                FuncSym func = (FuncSym) funcNameTable.get(name);
                if (func != null && unaryExp.getArgc() != func.getArgc()) {
                    errors.add(new Error("d", unaryExp.getLineno()));
                } else if (func != null) {
                    visitFuncParams(func.getFuncFParams(), unaryExp.getFuncRParams());
                }
            }
            nodeSymbolNap.put(unaryExp.getIdent(), getSymInStack(name, "Func"));
        } else if (unaryExp.isPrimaryExp()) {
            visitPrimaryExp(unaryExp.getPrimaryExp());
        } else {
            visitUnaryExp(unaryExp.getUnaryExp());
        }
    }

    public void visitPrimaryExp(PrimaryExp primaryExp) {
        AstNode node = primaryExp.getPrimaryExp();
        if (node instanceof Exp) {
            visitAddExp(((Exp) node).getAddExp());
        } else if (node instanceof LVal) {
            visitLVal((LVal) node);
        }
    }

    public void visitLVal(LVal lVal) {
        String name = lVal.getIdentName();
        if (!findSymInStack(name, "Var")) {
            errors.add(new Error("c", lVal.getIdent().getLineno()));
        }
        nodeSymbolNap.put(lVal.getIdent(), getSymInStack(name, "Var"));
        if (lVal.isArray()) {
            visitAddExp(lVal.getExp().getAddExp());
        }
    }

    public void visitFuncParams(FuncFParams ffp, FuncRParams frp) {
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
            visitAddExp(rp.getAddExp());
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
}
