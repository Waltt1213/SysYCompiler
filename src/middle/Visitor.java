package middle;

import frontend.Error;
import frontend.Token;
import frontend.TokenType;
import frontend.ast.*;
import frontend.ast.Character;
import frontend.ast.Number;
import llvmir.DataType;
import llvmir.Module;
import llvmir.Value;
import llvmir.values.*;
import llvmir.values.instr.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

import static llvmir.DataType.*;
import static llvmir.values.instr.Instruction.*;

public class Visitor {
    private final CompUnit root;
    private final ArrayList<SymbolTable> symbolTables;    //  符号表集
    private final ArrayDeque<SymbolTable> tableStack;     // 符号表栈
    private final ArrayDeque<Symbol> symStack;            // 符号栈
    private final Module module = new Module(VoidTy, "global");
    private final HashMap<String, Symbol> funcNameTable;
    private final ArrayDeque<BlockType> blockStack;
    private SymbolTable curTable;
    private int curDepth;
    private Function curFunction;
    private BasicBlock curBasicBlock;
    private final ArrayList<Error> errors;  // 错误处理

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
        this.errors = errors;
    }

    public void buildIR() {
        curTable = new SymbolTable(null, curDepth);
        tableStack.push(curTable);
        symbolTables.add(curTable);
        curFunction = null;
        curBasicBlock = null;
        buildDeclare();
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

    public Module getModule() {
        return module;
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

    public void buildDeclare() {
        Function getint = new Function(Integer32Ty,"getint", module, false);
        module.addDeclare(getint);

        Function getChar = new Function(Integer32Ty,"getchar", module, false);
        module.addDeclare(getChar);

        Function putint = new Function(VoidTy,"putint", module, false);
        module.addDeclare(putint);
        putint.addParams(new Argument(Integer32Ty, "param"));

        Function putch = new Function(VoidTy,"putch", module, false);
        module.addDeclare(putch);
        putch.addParams(new Argument(Integer32Ty, "param"));

        Function putstr = new Function(VoidTy,"putstr", module, false);
        module.addDeclare(putstr);
        putstr.addParams(new Argument(Pointer8Ty, "param"));
    }

    public void visitFuncDef(FuncDef fd) {
        SlotTracker.reset();
        String funcType = getType(fd.getFuncType().getType());
        SymType ft = new SymType(funcType, false, false, true);
        String funcName = fd.getIdent().getContent();
        try {
            // 创建函数并加入module
            Function function = new Function(getDataType(funcType), funcName, module, true);
            module.addFunction(function);
            // 加入符号表
            Symbol funcSym = new Symbol(funcName, ft, curDepth, fd.getFuncType().getType().getLineno());
            if (fd.hasFParams()) { funcSym.setFuncFParams(fd.getFuncFParams()); }
            funcSym.setValue(function);
            curFunction = function;
            symStack.push(funcSym);
            curTable.addSymItem(funcName, funcSym);
            funcNameTable.put(funcName, funcSym);
            // 创建一个基本块
            curBasicBlock = new BasicBlock(LabelTy, "");
            function.addBasicBlock(curBasicBlock);
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
        curBasicBlock.setName(SlotTracker.slot()); // 基本块占一个%
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
        // 归零
        SlotTracker.reset();
        // 创建main函数
        Function main = new Function(Integer32Ty, "main", module, true);
        curFunction = main;
        // 加入module
        module.addFunction(main);
        // 添加一个基本块
        curBasicBlock = new BasicBlock(LabelTy, SlotTracker.slot());
        main.addBasicBlock(curBasicBlock);
        if (!astNode.getBlock().hasRet()) {
            errors.add(new Error("g", (astNode).getBlock().getLineno()));
        }
        blockStack.push(BlockType.FuncBlock);

        visitBlock(astNode.getBlock(), null);
    }

    public void visitConstDef(String varType, ConstDef constDef) throws Error {
        String varName = constDef.getIdent().getContent();
        boolean isArray = constDef.hasArray();
        SymType st = new SymType(varType, isArray, true, false); //TODO
        Symbol var = new Symbol(varName, st, curDepth, constDef.getIdent().getLineno());
        // 加入符号表
        curTable.addSymItem(varName, var);
        symStack.push(var);
        // 判断是否为数组
        Value dim = new Constant(getDataType(varType),"0");
        String valueType = varType;
        if (constDef.hasArray()) {
            dim = visitConstExp(constDef.getConstExp());
            if (dim instanceof Constant) {
                valueType += "Array";
            }
        }
        // 判断是否为全局变量
        if (curTable.getFatherTable() == null) {
            GlobalVariable value = new GlobalVariable(getDataType(valueType), varName);
            module.addGlobalValue(value);
            if (dim instanceof Constant) {
                value.setDim((Constant) dim);
            }
            ArrayList<Value> constants = visitConstInitVal(constDef.getConstInitVal());
            value.setInitVal(constants);
        } else {
            // 局部变量先alloca
            Alloca alloca = new Alloca(getDataType(valueType), SlotTracker.slot());
            curBasicBlock.appendInstr(alloca);
            if (dim instanceof Constant) {
                alloca.setDim((Constant) dim);
            }
            // 再store初值
            ArrayList<Value> constants = visitConstInitVal(constDef.getConstInitVal());
            buildLocalInit(alloca, constants);
        }
    }

    public void buildLocalInit(Alloca alloca, ArrayList<Value> constants) {
        if (constants.size() == 1) {
            
        }
    }

    public Value visitConstExp(ConstExp constExp) {
        return visitAddExp(constExp.getAddExp());
    }

    public ArrayList<Value> visitConstInitVal(ConstInitVal constInitVal) {
        ArrayList<Value> constants = new ArrayList<>();
        if (constInitVal.isConstExp()) {
            constants.add(visitConstExp(constInitVal.getConstExp()));
        } else if (constInitVal.isConstExps()) {
            for (ConstExp constExp : constInitVal.getConstExps()) {
                constants.add(visitConstExp(constExp));
            }
        }
        return constants;
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
        SymType st = new SymType(varType, isArray,false, false); // TODO;
        InitVal iv = varDef.getInitVal();
        Symbol var = new Symbol(varName, st, curDepth, varDef.getIdent().getLineno());
        curTable.addSymItem(varName, var);
        symStack.push(var);
        // 判断是否为数组
        Value dim = new Constant(getDataType(varType),"0");
        String valueType = varType;
        if (varDef.hasConstExp()) {
            dim = visitConstExp(varDef.getConstExp());
            if (dim instanceof Constant) {
                valueType += "Array";
            }
        }
        if (curTable.getFatherTable() == null) {
            GlobalVariable globalVariable = new GlobalVariable(getDataType(valueType), varName);
            if (dim instanceof Constant) {
                globalVariable.setDim((Constant) dim);
            }
            module.addGlobalValue(globalVariable);
            if (varDef.hasInitVal()) {
                globalVariable.setInitVal(visitInitVal(varDef.getInitVal()));
            }
        }

    }

    public ArrayList<Value> visitInitVal(InitVal initVal) {
        ArrayList<Value> inits = new ArrayList<>();
        if (initVal.isExp()) {
            inits.add(visitAddExp(initVal.getExp().getAddExp()));
        } else if (initVal.isExps()) {
            for (Exp exp: initVal.getExps()) {
                inits.add(visitAddExp(exp.getAddExp()));
            }
        } else {
            inits.add(visitStringConst(initVal.getStringConst()));
        }
        return inits;
    }

    public Constant visitStringConst(Token string) {
        return new Constant(Pointer8Ty, string.getContent());
    }

    public void visitFuncFParams(FuncFParams fps) throws Error {
        for (AstNode node : fps.getAstChild()) {
            FuncFParam fp = (FuncFParam) node;
            String funcVarName = fp.getIdentName();
            String funcVarType = getType(fp.getType());
            // 创建参数并加入函数
            Argument argument = new Argument(getDataType(funcVarType), SlotTracker.slot());
            curFunction.addParams(argument);
            // 加入符号表
            SymType st = new SymType(funcVarType, fp.isArray(), false, false);
            Symbol symbol = new Symbol(funcVarName, st, curDepth, fp.getIdent().getLineno());
            curTable.addSymItem(funcVarName, symbol);
            symStack.push(symbol);
        }
    }

    public void visitStmt(Stmt stmt) {
        if (stmt.getType() == Stmt.StmtType.RETURN) {
            visitReturn(stmt);
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

    public void visitReturn(Stmt stmt) {
        if (curFunction.getTp().toString().equals("void") && !stmt.getStmts().isEmpty()) {
            errors.add(new Error("f", stmt.getLineno()));
        }
        if (!stmt.getStmts().isEmpty()) {
            Value ret = visitAddExp(((Exp) stmt.getStmts().get(0)).getAddExp());
            curBasicBlock.appendInstr(new Return(null, ret));
        } else {
            curBasicBlock.appendInstr(new Return(null));
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

    public Value getBinInstr(Value value1, Value value2, Type type) {
        if (value1 instanceof Constant && value2 instanceof Constant) {
            return calConst((Constant) value1, (Constant) value2, type);
        } else {
            Value zextValue1 = zext(value1);
            Value zextValue2 = zext(value2);
            BinaryOperator binOp = new BinaryOperator(Integer32Ty, type, SlotTracker.slot());
            binOp.addOperands(zextValue1);
            binOp.addOperands(zextValue2);
            curBasicBlock.appendInstr(binOp);
            return binOp;
        }
    }

    public Constant calConst(Constant value1, Constant value2, Type type) {
        switch (type) {
            case ADD:
                return new Constant(Integer.toString(Integer.parseInt(value1.toString())
                        + Integer.parseInt(value2.toString())));
            case SUB:
                return new Constant(Integer.toString(Integer.parseInt(value1.toString())
                        - Integer.parseInt(value2.toString())));
            case MUL:
                return new Constant(Integer.toString(Integer.parseInt(value1.toString())
                        * Integer.parseInt(value2.toString())));
            case SDIV:
                return new Constant(Integer.toString(Integer.parseInt(value1.toString())
                        / Integer.parseInt(value2.toString())));
            case SREM:
                return new Constant(Integer.toString(Integer.parseInt(value1.toString())
                        % Integer.parseInt(value2.toString())));
            default:
                return null;
        }
    }

    public Value zext(Value value) {
        if (value.getTp() != Integer32Ty) {
            Zext zext = new Zext(value.getTp(), Instruction.Type.ZEXT, SlotTracker.slot());
            zext.addOperands(value);
            curBasicBlock.appendInstr(zext);
            return zext;
        }
        return value;
    }

    public Value visitAddExp(AddExp addExp) {
        if (addExp.getMulExps().size() == 1) {
            return visitMulExp(addExp.getMulExps().get(0));
        }
        Value res = null;
        for (int i = 1; i < addExp.getMulExps().size(); i++) {
            Value value1 = i == 1 ? visitMulExp(addExp.getMulExps().get(0)) : res;
            Value value2 = visitMulExp(addExp.getMulExps().get(i));
            // 如果value1 和 value2 是Const
            res = getBinInstr(value1, value2, Type.getOp(addExp.getOps().get(i - 1).getContent()));
        }
        return res;
    }

    public Value visitMulExp(MulExp mulExp) {
        if (mulExp.getUnaryExps().size() == 1) {
            return visitUnaryExp(mulExp.getUnaryExps().get(0));
        }
        Value res = null;
        for (int i = 1; i < mulExp.getUnaryExps().size(); i++) {
            Value value1 = i == 1 ? visitUnaryExp(mulExp.getUnaryExps().get(0)) : res;
            Value value2 = visitUnaryExp(mulExp.getUnaryExps().get(i));
            // 如果value1 和 value2 是Const
            res = getBinInstr(value1, value2, Type.getOp(mulExp.getOps().get(i - 1).getContent()));
        }
        return res;
    }

    public Value visitUnaryExp(UnaryExp unaryExp) {
        if (unaryExp.isIdent()) {
            String name = unaryExp.getIdentName();
            if (!findSymInStack(name, "Func")) {
                errors.add(new Error("c", unaryExp.getIdent().getLineno()));
            }
            if (unaryExp.hasFuncRParams() && funcNameTable.containsKey(name)) {
                Symbol func = funcNameTable.get(name);
                if (func != null && unaryExp.getArgc() != ((Function) func.getValue()).getArgc()) {
                    errors.add(new Error("d", unaryExp.getLineno()));
                } else if (func != null) {
                    visitFuncRParams(func.getFuncFParams(), unaryExp.getFuncRParams());
                }
            }
        } else if (unaryExp.isPrimaryExp()) {
            return visitPrimaryExp(unaryExp.getPrimaryExp());
        } else { //
            if (unaryExp.getUnaryOp().equals("-")) {
                Value value1 = new Constant("0");
                Value value2 = visitUnaryExp(unaryExp.getUnaryExp());
                return getBinInstr(value1, value2, Type.SUB);
            } else if (unaryExp.getUnaryOp().equals("+")) {
                return visitUnaryExp(unaryExp.getUnaryExp());
            } else { // 取反操作就是与0比较是否相等
                Value value1 = visitUnaryExp(unaryExp.getUnaryExp());
                Value value2 = new Constant("0");
                Compare compare = new Compare(Integer32Ty, SlotTracker.slot(), "!=");
                compare.addOperands(value1);
                compare.addOperands(value2);
                curBasicBlock.appendInstr(compare);
                return compare;
            }
        }
        return null;
    }

    public Value visitPrimaryExp(PrimaryExp primaryExp) {
        AstNode node = primaryExp.getPrimaryExp();
        if (node instanceof Exp) {
            visitAddExp(((Exp) node).getAddExp());
        } else if (node instanceof LVal) {
            visitLVal((LVal) node);
        } else if (node instanceof Number) {
            return new Constant(((Number) node).getNumber());
        } else if (node instanceof Character) {
            return new Constant(Integer8Ty, ((Character) node).getChar());
        }
        return null;
    }

    public void visitLVal(LVal lVal) {
        String name = lVal.getIdentName();
        if (!findSymInStack(name, "Var")) {
            errors.add(new Error("c", lVal.getIdent().getLineno()));
        }
        if (lVal.isArray()) {
            visitAddExp(lVal.getExp().getAddExp());
        }
    }

    public void visitFuncRParams(FuncFParams ffp, FuncRParams frp) {
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

    public DataType getDataType(String type) {
        if (type.equals("Int")) {
            return Integer32Ty;
        } else if (type.equals("Char")) {
            return Integer8Ty;
        } else if (type.equals("IntArray")) {
            return Pointer32Ty;
        } else if (type.equals("CharArray")) {
            return Pointer8Ty;
        } else {
            return VoidTy;
        }
    }
}
