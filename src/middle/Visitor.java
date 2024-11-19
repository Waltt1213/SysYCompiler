package middle;

import frontend.Error;
import frontend.Token;
import frontend.TokenType;
import frontend.ast.*;
import frontend.ast.Character;
import frontend.ast.Number;
import llvmir.Module;
import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.*;
import llvmir.values.instr.*;
import utils.Transform;

import java.util.ArrayDeque;
import java.util.ArrayList;

import static llvmir.ValueType.DataType.*;
import static llvmir.values.instr.Instruction.*;

public class Visitor {
    private final CompUnit root;
    private final ArrayList<SymbolTable> symbolTables;    //  符号表集
    private final Module module = new Module(new ValueType.Type(VoidTy), "global");
    private SymbolTable curTable;
    private int curDepth;
    private Function curFunction;
    private BasicBlock curBasicBlock;
    private final ArrayDeque<BasicBlock.ForBlock> loopBlockStack; // TODO： 设置成栈
    private final ArrayList<Error> errors;  // 错误处理

    public Visitor(CompUnit compUnit, ArrayList<Error> errors) {
        this.root = compUnit;
        curDepth = 1;
        symbolTables = new ArrayList<>();
        loopBlockStack = new ArrayDeque<>();
        this.errors = errors;
    }

    public void buildIR() {
        curTable = new SymbolTable(null, curDepth);
        symbolTables.add(curTable);
        curFunction = null;
        curBasicBlock = null;
        visitCompUnit(root);
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

    public void visitFuncDef(FuncDef fd) {
        // SlotTracker.reset();
        String funcType = getType(fd.getFuncType().getType());
        SymType ft = new SymType(funcType, false, false, true);
        String funcName = fd.getIdent().getContent();
        Function function = new Function(getDataType(funcType), funcName, true);
        // 创建函数并加入module
        module.addFunction(function);
        try {
            // 加入符号表
            Symbol funcSym = new Symbol(funcName, ft, curDepth, fd.getFuncType().getType().getLineno());
            if (fd.hasFParams()) { funcSym.setFuncFParams(fd.getFuncFParams()); }
            funcSym.setValue(function);
            curFunction = function;
            curTable.addSymItem(funcName, funcSym);
        } catch (Error e) {
            errors.add(new Error("b", fd.getFuncType().getType().getLineno()));
        }
        // 创建一个基本块
        curBasicBlock = new BasicBlock("", curFunction);
        function.addBasicBlock(curBasicBlock);
        // 形参需要加入符号表，但属于下一层级
        curDepth++;
        SymbolTable st = new SymbolTable(curTable, curDepth);
        curTable.addChildren(st);   // 新表设为当前表的孩子
        curTable = st;
        symbolTables.add(curTable); // 将符号表加入符号表集
        ArrayList<Symbol> paramsSym = new ArrayList<>();
        if (fd.getFuncFParams() != null) {
            try {
                paramsSym = visitFuncFParams(fd.getFuncFParams());  // 函数形参与函数块内符号属于同一层级
            } catch (Error e) { errors.add(e); }
        }
        // curBasicBlock.setName(SlotTracker.slot()); // 基本块占一个%
        buildParamInit(paramsSym);
        visitBlock(fd.getBlock());  // 构建函数内符号表
        if (!ft.toString().equals("VoidFunc") && !fd.getBlock().hasRet()) {
            errors.add(new Error("g", fd.getBlock().getLineno()));
        }
        if (!curFunction.isReturn()) {
            curFunction.setReturn(curBasicBlock);
        }
    }

    public void visitBlock(Block block) {
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
        curTable = curTable.getFatherTable();
    }

    public void visitMainFuncDef(MainFuncDef astNode) {
        // 归零
        // SlotTracker.reset();
        // 创建main函数
        Function main = new Function(new ValueType.Type(Integer32Ty), "main", true);
        curFunction = main;
        // 加入module
        module.addFunction(main);
        // 添加一个基本块
        curBasicBlock = new BasicBlock("", curFunction);
        main.addBasicBlock(curBasicBlock);
        if (!astNode.getBlock().hasRet()) {
            errors.add(new Error("g", (astNode).getBlock().getLineno()));
        }
        // 进入下一层
        curDepth++;
        SymbolTable st = new SymbolTable(curTable, curDepth);
        curTable.addChildren(st);   // 新表设为当前表的孩子
        curTable = st;
        symbolTables.add(curTable); // 将符号表加入符号表集
        visitBlock(astNode.getBlock());
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

    public void visitConstDef(String varType, ConstDef constDef) throws Error {
        String varName = constDef.getIdent().getContent();
        boolean isArray = constDef.hasArray();
        SymType st = new SymType(varType, isArray, true, false); //TODO
        Symbol var = new Symbol(varName, st, curDepth, constDef.getIdent().getLineno());
        // 加入符号表
        curTable.addSymItem(varName, var);
        // 判断是否为数组
        ValueType.Type type = getDataType(varType);
        if (constDef.hasArray()) {
            Value dim = visitConstExp(constDef.getConstExp());
            type = getDataType(varType + "Array");
            ((ValueType.ArrayType) type).setDim(Integer.parseInt(dim.getName()));
        }
        // 判断是否为全局变量
        if (curTable.getFatherTable() == null) { // 全局变量
            GlobalVariable value = new GlobalVariable(type, varName);
            module.addGlobalValue(value);
            ArrayList<Value> constants = visitConstInitVal(constDef.getConstInitVal(), type.getDataType());
            value.setInitVal(constants);
            var.setValue(value);
            value.setConstant(true);
        } else {
            // 局部变量先alloca
            Alloca alloca = new Alloca(type, "");
            curBasicBlock.appendInstr(alloca, true);
            // 再store初值
            ArrayList<Value> constants = visitConstInitVal(constDef.getConstInitVal(), type.getDataType());
            buildLocalInit(alloca, constants, true, null);
            var.setValue(alloca);
        }
    }

    public void buildLocalInit(Alloca alloca, ArrayList<Value> inits, boolean isConst, InitVal initVal) { // TODO: 有大问题
        if (inits.size() == 1) { // 单个常量 or 字符串
            Value value = inits.get(0);
            if (value instanceof Constant && ((Constant) value).isString()) {
                String s = inits.get(0).getName();
                //s = s.substring(1, s.length() - 1); // 删去“”
                ArrayList<Integer> str2int = Transform.str2intList(s);
                Value prePtr = alloca;
                for (int i = 0; i < alloca.getDim() && !str2int.isEmpty(); i++) {
                    GetElementPtr getElementPtr = new GetElementPtr(prePtr.getTp(), "");
                    getElementPtr.addOperands(prePtr);
                    if (i == 0) {
                        getElementPtr.addOperands(new Constant("0"));
                        getElementPtr.addOperands(new Constant("0"));
                    } else {
                        getElementPtr.addOperands(new Constant("1"));
                    }
                    curBasicBlock.appendInstr(getElementPtr, true);
                    Store store = new Store(new ValueType.Type(VoidTy), "");
                    Value res;
                    if (i < str2int.size()) {
                        res = new Constant(new ValueType.Type(Integer8Ty), String.valueOf(str2int.get(i)));
                        store.addOperands(res);
                    } else {
                        res = new Constant(new ValueType.Type(Integer8Ty), "0");
                        store.addOperands(res);
                    }
                    store.addOperands(getElementPtr);
                    if (isConst) {
                        alloca.setConst(true);
                        alloca.addConstInit(res);
                    }
                    curBasicBlock.appendInstr(store, false);
                    prePtr = getElementPtr;
                }
            } else if (alloca.isArray()) {
                GetElementPtr getElementPtr = new GetElementPtr(alloca.getTp(), "");
                getElementPtr.addOperands(alloca);
                getElementPtr.addOperands(new Constant("0"));
                getElementPtr.addOperands(new Constant("0"));
                curBasicBlock.appendInstr(getElementPtr, true);
                Value res;
                if (initVal != null) {
                    Value init;
                    if (initVal.isExp()) {
                        init = visitAddExp(initVal.getExp().getAddExp());
                    } else {
                        init = visitAddExp(initVal.getExps().get(0).getAddExp());
                    }
                    res = buildInit(getElementPtr, init);
                } else {
                    res = buildInit(getElementPtr, inits.get(0));
                }
                if (isConst) {
                    alloca.setConst(true);
                    alloca.addConstInit(res);
                }
            } else {
                Value res;
                if (initVal != null) {
                    Value init;
                    if (initVal.isExp()) {
                        init = visitAddExp(initVal.getExp().getAddExp());
                    } else {
                        init = visitAddExp(initVal.getExps().get(0).getAddExp());
                    }
                    res = buildInit(alloca, init);
                } else {
                    res = buildInit(alloca, inits.get(0));
                }
                if (isConst) {
                    alloca.setConst(true);
                    alloca.addConstInit(res);
                }
            }
        } else { // 数组
            Value prePtr = alloca;
            for (int i = 0;  i < alloca.getDim(); i++) {
                GetElementPtr getElementPtr = new GetElementPtr(prePtr.getTp(),"");
                getElementPtr.addOperands(prePtr);
                if (i == 0) {
                    getElementPtr.addOperands(new Constant("0"));
                    getElementPtr.addOperands(new Constant("0"));
                } else {
                    getElementPtr.addOperands(new Constant("1"));
                }
                Value res = null;
                if (i < inits.size()) {
                    // init.get(i) = visitAddExp(initVal.getExps().get(i).getAddExp())
                    if (initVal == null) {
                        curBasicBlock.appendInstr(getElementPtr, true);
                        res = buildInit(getElementPtr, inits.get(i));
                    } else {
                        Value init = visitAddExp(initVal.getExps().get(i).getAddExp());
                        curBasicBlock.appendInstr(getElementPtr, true);
                        res = buildInit(getElementPtr, init);
                    }
                } else if (isConst) {
                    curBasicBlock.appendInstr(getElementPtr, true);
                    res = buildInit(getElementPtr, new Constant(new ValueType.Type(alloca.getDataType()), "0"));
                }
                if (isConst) {
                    alloca.setConst(true);
                    alloca.addConstInit(res);
                }
                prePtr = getElementPtr;
            }
        }
    }

    public Value buildInit(Value value1, Value value2) { // value1 = value2
        Store store = new Store(new ValueType.Type(VoidTy), "");
        // TODO
        Value res = value2;
        if (value2.getTp().getDataType().compareTo(value1.getTp().getDataType()) > 0) {
            // 需要trunc
            res = trunc(value2);
        } else if (value2.getTp().getDataType().compareTo(value1.getTp().getDataType()) < 0) {
            res = zext(value2);
        }
        store.addOperands(res);
        store.addOperands(value1);
        curBasicBlock.appendInstr(store, false);
        return res;
    }

    public Constant visitConstExp(ConstExp constExp) {
        return (Constant) visitAddExp(constExp.getAddExp());
    }

    public ArrayList<Value> visitConstInitVal(ConstInitVal constInitVal, ValueType.DataType type) {
        ArrayList<Value> constants = new ArrayList<>();
        if (constInitVal.isConstExp()) {
            Constant constant = visitConstExp(constInitVal.getConstExp()).deepClone();
            constant.setTp(new ValueType.Type(type));
            constants.add(constant);
        } else if (constInitVal.isConstExps()) {
            for (ConstExp constExp : constInitVal.getConstExps()) {
                Constant constant = visitConstExp(constExp).deepClone();
                constant.setTp(new ValueType.Type(type));
                constants.add(constant);
            }
        } else {
            Constant constant = visitStringConst(constInitVal.getStringConst()).deepClone();
            constants.add(constant);
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
        Symbol var = new Symbol(varName, st, curDepth, varDef.getIdent().getLineno());
        curTable.addSymItem(varName, var);
        // 判断是否为数组
        ValueType.Type type = getDataType(varType);
        if (varDef.hasConstExp()) {
            Value dim = visitConstExp(varDef.getConstExp());
            type = getDataType(varType + "Array");
            ((ValueType.ArrayType) type).setDim(Integer.parseInt(dim.getName()));
        }
        if (curTable.getFatherTable() == null) {
            GlobalVariable globalVariable = new GlobalVariable(type, varName);
            module.addGlobalValue(globalVariable);
            if (varDef.hasInitVal()) {
                globalVariable.setInitVal(visitInitVal(varDef.getInitVal()));
            }
            var.setValue(globalVariable);
        } else {
            // 局部变量先alloca
            Alloca alloca = new Alloca(type, "");
            curBasicBlock.appendInstr(alloca, true);
            // 再store初值
            if (varDef.hasInitVal()) {
                boolean oldTer = curBasicBlock.isTerminator();
                curBasicBlock.setTerminator(true);
                ArrayList<Value> inits = visitInitVal(varDef.getInitVal());
                curBasicBlock.setTerminator(oldTer);
                buildLocalInit(alloca, inits, false, varDef.getInitVal());
            }
            var.setValue(alloca);
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
        Constant constant = new Constant(new ValueType.ArrayType(Integer8Ty),
                string.getContent().substring(1, string.getContent().length() - 1));
        constant.setString(true);
        return constant;
    }

    public ArrayList<Symbol> visitFuncFParams(FuncFParams fps) throws Error {
        ArrayList<Symbol> paramsSym = new ArrayList<>();
        for (AstNode node : fps.getAstChild()) {
            FuncFParam fp = (FuncFParam) node;
            String funcVarName = fp.getIdentName();
            String funcVarType = getType(fp.getType());
            if (fp.isArray()) {
                funcVarType += "Pointer";
            }
            // 创建参数并加入函数
            Argument argument = new Argument(getDataType(funcVarType), "");
            curFunction.addParams(argument, true);
            SymType st = new SymType(getType(fp.getType()), fp.isArray(), false, false);
            Symbol symbol = new Symbol(funcVarName, st, curDepth, fp.getIdent().getLineno());
            curTable.addSymItem(funcVarName, symbol);
            paramsSym.add(symbol);
        }
        return paramsSym;
    }

    public void buildParamInit(ArrayList<Symbol> paramsSym) {
        for (Argument argument: curFunction.getFuncFParams()) {
            Alloca alloca = new Alloca(argument.getTp(), "");
            curBasicBlock.appendInstr(alloca, true);
            Store store = new Store(new ValueType.Type(VoidTy), "");
            store.addOperands(argument);
            store.addOperands(alloca);
            curBasicBlock.appendInstr(store, false);
            if (!paramsSym.isEmpty()) {
                Symbol symbol = paramsSym.remove(0);
                symbol.setValue(alloca);
            }
        }
    }

    public void visitStmt(Stmt stmt) {
        if (stmt.getType() == Stmt.StmtType.RETURN) {
            visitReturn(stmt);
        } else if (stmt.getType() == Stmt.StmtType.IF) {
            visitIf(stmt);
        } else if (stmt.getType() == Stmt.StmtType.FOR) {
            visitFor(stmt);
        } else if (stmt.getType() == Stmt.StmtType.BREAK) {
            visitBreak(stmt);
        } else if (stmt.getType() == Stmt.StmtType.CONTINUE) {
            visitContinue(stmt);
        } else if (stmt.getType() == Stmt.StmtType.PRINTF) {
            visitPrintf(stmt);
        } else if (stmt.getType() == Stmt.StmtType.EXP) {
            Value value = visitAddExp(((Exp) stmt.getStmts().get(0)).getAddExp());
        } else if (stmt.getType() == Stmt.StmtType.BLOCK) {
            curDepth++;
            SymbolTable st = new SymbolTable(curTable, curDepth);
            curTable.addChildren(st);   // 新表设为当前表的孩子
            curTable = st;
            symbolTables.add(curTable); // 将符号表加入符号表集
            visitBlock((Block) stmt.getStmts().get(0));
        } else if (!stmt.getStmts().isEmpty() && stmt.getStmts().get(0) instanceof LVal) {
            String name = ((LVal) stmt.getStmts().get(0)).getIdentName();
            Symbol sym;
            if ((sym = curTable.findSym(name, "Var")) != null) {
                if (sym.getType().isConst()) {
                    if (stmt.iaNormalAssign()) {
                        errors.add(new Error("h", stmt.getLineno()));
                    }
                }
            }
            Value value = visitLVal((LVal) stmt.getStmts().get(0), false);
            if (stmt.getType() == Stmt.StmtType.ASSIGN) {
                Value res = visitAddExp(((Exp) stmt.getStmts().get(1)).getAddExp());
                if (value.getTp().getDataType() == Integer32Ty) {
                    res = zext(res);
                } else {
                    res = trunc(res);
                }
                Store store = new Store(new ValueType.Type(VoidTy), "");
                store.addOperands(res);
                store.addOperands(value);
                curBasicBlock.appendInstr(store, false);
            } else if (stmt.getType() == Stmt.StmtType.GETINT) {
                visitGetInt(value);
            } else if (stmt.getType() == Stmt.StmtType.GETCHAR) {
                visitGetChar(value);
            }
        }
    }

    public void visitGetInt(Value value) {
        Function getint = module.getDeclare("getint");
        if (getint == null) {
            getint = new Function(new ValueType.Type(Integer32Ty), "getint", false);
            getint.setNotVoid(true);
            module.addDeclare(getint);
        }
        Call call = new Call("", getint);
        curBasicBlock.appendInstr(call, true);
        Value value1 = zext(call);
        Store store = new Store(new ValueType.Type(VoidTy), "");
        store.addOperands(value1);
        store.addOperands(value);
        curBasicBlock.appendInstr(store, false);
    }

    public void visitGetChar(Value value) {
        Function getchar = module.getDeclare("getchar");
        if (getchar == null) {
            getchar = new Function(new ValueType.Type(Integer32Ty), "getchar", false);
            getchar.setNotVoid(true);
            module.addDeclare(getchar);
        }
        Call call = new Call("", getchar);
        curBasicBlock.appendInstr(call, true);
        Value value1 = trunc(call);
        Store store = new Store(new ValueType.Type(VoidTy), "");
        store.addOperands(value1);
        store.addOperands(value);
        curBasicBlock.appendInstr(store, false);
    }

    public void visitReturn(Stmt stmt) {
        if (curFunction.getTp().toString().equals("void") && !stmt.getStmts().isEmpty()) {
            errors.add(new Error("f", stmt.getLineno()));
        }
        if (!stmt.getStmts().isEmpty()) {
            Value ret = visitAddExp(((Exp) stmt.getStmts().get(0)).getAddExp());
            curFunction.setNotVoid(true);
            // curBasicBlock.appendInstr(new Return(null, ret));
            Value res = ret;
            if (ret.getTp().getDataType().compareTo(curFunction.getTp().getDataType()) > 0) {
                // 需要trunc
                res = trunc(ret);
            } else if (ret.getTp().getDataType().compareTo(curFunction.getTp().getDataType()) < 0) {
                res = zext(ret);
            }
            curFunction.setReturn(res, curBasicBlock);
        } else {
            // curBasicBlock.appendInstr(new Return(null));
            curFunction.setReturn(curBasicBlock);
        }
    }

    public void visitIf(Stmt stmt) {
        BasicBlock ifBlock = new BasicBlock("", curFunction);
        BasicBlock endBlock = new BasicBlock("", curFunction);
        BasicBlock elseBlock = null;
        if (stmt.getStmts().size() > 2) {
            elseBlock = new BasicBlock("", curFunction);
        }
        // 进入条件判断
        visitCond((Cond) stmt.getStmts().get(0), ifBlock, endBlock, elseBlock);
        // ifBlock.setName(SlotTracker.slot());
        curFunction.addBasicBlock(ifBlock);
        // 进入ifBlock
        curBasicBlock = ifBlock;
        visitStmt((Stmt) stmt.getStmts().get(1));
        Branch out = new Branch("");
        out.addOperands(endBlock);
        curBasicBlock.setTerminator(out);
        endBlock.setLabeled(true);
        // 进入else
        if (stmt.getStmts().size() > 2 && elseBlock != null) {
            // elseBlock.setName(SlotTracker.slot());
            curFunction.addBasicBlock(elseBlock);
            curBasicBlock = elseBlock;
            visitStmt((Stmt) stmt.getStmts().get(2));
            Branch out2 = new Branch("");
            out2.addOperands(endBlock);
            curBasicBlock.setTerminator(out2);
            endBlock.setLabeled(true);
        }
        // endBlock.setName(SlotTracker.slot());
        curBasicBlock = endBlock;
        curFunction.addBasicBlock(endBlock);
    }

    public void visitCond(Cond cond, BasicBlock ifBlock, BasicBlock endBlock, BasicBlock elseBlock) {
        ArrayList<BasicBlock> blocks = new ArrayList<>();
        blocks.add(ifBlock);
        blocks.add(endBlock);
        blocks.add(elseBlock);
        visitLOrExp(cond.getLorExp(), blocks);
    }

    public void visitFor(Stmt stmt) {
        BasicBlock outBlock = new BasicBlock("", curFunction);
        BasicBlock.ForBlock condBlock = new BasicBlock.ForBlock("", outBlock, curFunction);
        BasicBlock updateBlock = null;
        if (stmt.getStmts().get(2) != null) {
            updateBlock = new BasicBlock("", curFunction);
            condBlock.setUpdateBlock(updateBlock);
        }
        // 初始化
        if (stmt.getStmts().get(0) != null) {
            visitForStmt((ForStmt) stmt.getStmts().get(0)); // 执行第一个ForStmt
        }
        // 进入条件判断
        BasicBlock judgeBlock = new BasicBlock("", curFunction);
        Branch judge = new Branch("");
        if (stmt.getStmts().get(1) != null) {
            judge.addOperands(judgeBlock);
            curBasicBlock.setTerminator(judge);
            curBasicBlock = judgeBlock;
            curFunction.addBasicBlock(judgeBlock);
            // judgeBlock.setName(SlotTracker.slot());
            visitCond((Cond) stmt.getStmts().get(1), condBlock, outBlock, null);
            if (updateBlock == null) {
                condBlock.setUpdateBlock(judgeBlock);
            }
        } else {
            judge.addOperands(condBlock);
            curBasicBlock.setTerminator(judge);
        }
        // 进入条件执行体
        // condBlock.setName(SlotTracker.slot());
        curBasicBlock = condBlock;
        curFunction.addBasicBlock(condBlock);
        loopBlockStack.push(condBlock);
        visitStmt((Stmt) stmt.getStmts().get(3));
        // 跳转到continueBlock
        if (updateBlock != null) {
            updateBlock.setLabeled(true);
            // updateBlock.setName(SlotTracker.slot());
            Branch join = new Branch("");
            join.addOperands(updateBlock);
            curBasicBlock.setTerminator(join);
            curFunction.addBasicBlock(updateBlock);
            curBasicBlock = updateBlock;
            visitForStmt((ForStmt) stmt.getStmts().get(2)); // 执行第一个ForStmt
        }
        // 继续跳转到判断语句或执行体
        Branch next = new Branch("");
        if (stmt.getStmts().get(1) != null) {
            next.addOperands(judgeBlock);
            judgeBlock.setLabeled(true);
        } else {
            next.addOperands(condBlock);
            condBlock.setLabeled(true);
        }
        curBasicBlock.setTerminator(next);
        // 进入结束块
        // outBlock.setName(SlotTracker.slot());
        curBasicBlock = outBlock;
        curFunction.addBasicBlock(outBlock);
        loopBlockStack.pop();
    }

    public void visitBreak(Stmt stmt) {
        if (loopBlockStack.isEmpty()) {
            errors.add(new Error("m", stmt.getLineno()));
            return;
        }
        Branch out = new Branch("");
        BasicBlock basicBlock = null;
        if (loopBlockStack.peek() != null) {
            basicBlock = loopBlockStack.peek().getOutBlock();
            out.addOperands(basicBlock);
            basicBlock.setLabeled(true);
        }
        curBasicBlock.setTerminator(out);
    }

    public void visitContinue(Stmt stmt) {
        if (loopBlockStack.isEmpty()) {
            errors.add(new Error("m", stmt.getLineno()));
            return;
        }
        Branch out = new Branch("");
        BasicBlock basicBlock = null;
        if (loopBlockStack.peek() != null) {
            basicBlock = loopBlockStack.peek().getUpdateBlock();
            out.addOperands(basicBlock);
            basicBlock.setLabeled(true);
        }
        curBasicBlock.setTerminator(out);
    }

    public void visitPrintf(Stmt stmt) {
        if (stmt.FormatNum() != stmt.getStmts().size()) {
            errors.add(new Error("l", stmt.getLineno()));
            for (AstNode exp: stmt.getStmts()) {
                visitAddExp(((Exp) exp).getAddExp());
            }
            return;
        }
        ArrayList<Value> puts = new ArrayList<>();
        for (AstNode exp: stmt.getStmts()) {
            puts.add(visitAddExp(((Exp) exp).getAddExp()));
        }
        int index = 0;
        String strConst = stmt.getStringConst();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < strConst.length() - 1; i++) {
            char c = strConst.charAt(i);
            if (c == '%' && (strConst.charAt(i + 1) == 'd' || strConst.charAt(i + 1) == 'c')) {
                String globalStr = sb.toString();
                // System.out.println(globalStr);
                sb.setLength(0);
                if (!globalStr.isEmpty()) {
                    ValueType.ArrayType arrayType = new ValueType.ArrayType(Integer8Ty);
                    arrayType.setDim(Transform.charList2string(globalStr).length() + 1);
                    GlobalVariable var = new GlobalVariable(arrayType, SlotTracker.slotStr());
                    var.setUnnamed(true);
                    var.setConstant(true);
                    var.addInitVal(new Constant(new ValueType.ArrayType(Integer8Ty), globalStr));
                    var.setString(true);
                    module.addGlobalValue(var);
                    buildPrintf(var, 's'); // 打印字符串
                }
                i++;
                buildPrintf(puts.get(index), strConst.charAt(i));
                index++;
            } else {
                sb.append(c);
            }
        }
        String globalStr = sb.toString();
        sb.setLength(0);
        if (!globalStr.isEmpty()) {
            ValueType.ArrayType arrayType = new ValueType.ArrayType(Integer8Ty);
            arrayType.setDim(Transform.charList2string(globalStr).length() + 1);
            GlobalVariable var = new GlobalVariable(arrayType, SlotTracker.slotStr());
            var.setUnnamed(true);
            var.setConstant(true);
            var.setString(true);
            var.addInitVal(new Constant(new ValueType.ArrayType(Integer8Ty), globalStr));
            module.addGlobalValue(var);
            buildPrintf(var, 's'); // 打印字符串
        }
    }

    public void buildPrintf(Value var, java.lang.Character c) {
        if (c == 's') {
            Function putstr = module.getDeclare("putstr");
            if (putstr == null) {
                putstr = new Function(new ValueType.Type(VoidTy),"putstr", false);
                module.addDeclare(putstr);
                putstr.addParams(new Argument(new ValueType.PointerType(Integer8Ty), "param"), false);
            }
            Call call = new Call(putstr);
            curBasicBlock.appendInstr(call, false);
            GetElementPtr getElementPtr = new GetElementPtr(var.getTp(), null);
            getElementPtr.addOperands(var);
            getElementPtr.addOperands(new Constant(new ValueType.Type(Integer64Ty), "0"));
            getElementPtr.addOperands(new Constant(new ValueType.Type(Integer64Ty), "0"));
            call.addFuncRParam(getElementPtr);
        } else if (c == 'c') {
            Function putch = module.getDeclare("putch");
            if (putch == null) {
                putch = new Function(new ValueType.Type(VoidTy), "putch", false);
                module.addDeclare(putch);
                putch.addParams(new Argument(new ValueType.Type(Integer32Ty), "param"), false);
            }
            Call call = new Call(putch);
            Value newVar = zext(var);
            call.addFuncRParam(newVar);
            curBasicBlock.appendInstr(call, false);
        } else {
            Function putint = module.getDeclare("putint");
            if (putint == null) {
                putint = new Function(new ValueType.Type(VoidTy), "putint", false);
                module.addDeclare(putint);
                putint.addParams(new Argument(new ValueType.Type(Integer32Ty), "param"), false);
            }
            Call call = new Call(putint);
            Value newVar = zext(var);
            call.addFuncRParam(newVar);
            curBasicBlock.appendInstr(call, false);
        }
    }

    public void visitForStmt(ForStmt forStmt) {
        String name = forStmt.getLval().getIdentName();
        Symbol sym;
        if ((sym = curTable.findSym(name, "Var")) != null) {
            //Symbol sym = getSymInStack(name, "Var");
            if (sym.getType().isConst()) {
                errors.add(new Error("h", forStmt.getLineno()));
            }
        }
        Value value = visitLVal(forStmt.getLval(), false);
        Value res = visitAddExp(forStmt.getExp().getAddExp());
        if (value.getTp().getDataType() == Integer32Ty) {
            res = zext(res);
        } else {
            res = trunc(res);
        }
        Store store = new Store(new ValueType.Type(VoidTy), "");
        store.addOperands(res);
        store.addOperands(value);
        curBasicBlock.appendInstr(store, false);
    }

    public void visitLOrExp(LOrExp lOrExp, ArrayList<BasicBlock> blocks) {
        BasicBlock next; // 表示后面还有或判断
        for (int i = 0; i < lOrExp.getLAndExps().size(); i++) {
            if (i < lOrExp.getLAndExps().size() - 1) {
                next = new BasicBlock("", curFunction);
            } else {
                next = null;
            }
            blocks.add(3, next);
            Value value = visitLAndExp(lOrExp.getLAndExps().get(i), blocks);
            if (value.getTp().getDataType() != Integer1Ty) {
                // 不为0则为真
                Compare compare = new Compare("", Compare.CondType.NE);
                compare.addOperands(value);
                compare.addOperands(new Constant("0"));
                curBasicBlock.appendInstr(compare, true);
                value = compare;
            }
            BasicBlock falseBlock = null;
            if (i == lOrExp.getLAndExps().size() - 1) {
                if (blocks.get(2) == null) { // 如果没有else
                    falseBlock = blocks.get(1); // false则跳转至endBlock
                } else {
                    falseBlock = blocks.get(2); // false则跳转至elseBlock
                }
            } else if (next != null) { // 后面还有或语句
                // next.setName(SlotTracker.slot());
                falseBlock = next;
                curFunction.addBasicBlock(next);
            }
            if (i != lOrExp.getLAndExps().size() - 1
                    || lOrExp.getLAndExps().get(i).getEqExps().size() <= 1) {
                Branch branch = new Branch("");
                branch.addOperands(value);
                branch.addOperands(blocks.get(0));
                branch.addOperands(falseBlock);
                curBasicBlock.setTerminator(branch);
                if (falseBlock != null) {
                    falseBlock.setLabeled(true);
                }
                blocks.get(0).setLabeled(true);
            }
            curBasicBlock = falseBlock;
        }
    }

    public Value visitLAndExp(LAndExp lAndExp, ArrayList<BasicBlock> blocks) {
        if (lAndExp.getEqExps().size() == 1) {
            return visitEqExp(lAndExp.getEqExps().get(0));
        }
        Value value = null;
        for (int i = 0; i < lAndExp.getEqExps().size(); i++) {
            value = visitEqExp(lAndExp.getEqExps().get(i));
            if (value.getTp().getDataType() != Integer1Ty) {
                // 不为0则为真
                Compare compare = new Compare("", Compare.CondType.NE);
                compare.addOperands(value);
                compare.addOperands(new Constant("0"));
                curBasicBlock.appendInstr(compare, true);
                value = compare;
            }
            BasicBlock tureBlock = null;
            // 如果后面没有与判断，如果为真则跳转到ifBlock
            // 如果后面还有判断，则跳转到下一个基本块中判断
            if (i == lAndExp.getEqExps().size() - 1) {
                tureBlock = blocks.get(0);
            } else {
                BasicBlock judge = new BasicBlock("", curFunction); // 新的基本块做下一个与判断
                tureBlock = judge;
                curFunction.addBasicBlock(judge);
            }
            Branch branch = new Branch("");
            branch.addOperands(value);
            branch.addOperands(tureBlock);
            // 如果后面还有或判断，则如果为假继续判断
            // 如果后面无或判断，则跳到else或者end
            if (blocks.get(3) != null) {
                branch.addOperands(blocks.get(3));  // else judgeBlock
                blocks.get(3).setLabeled(true);
            } else if (blocks.get(2) != null) {
                branch.addOperands(blocks.get(2));  // else elseblock
                blocks.get(2).setLabeled(true);
            } else {
                branch.addOperands(blocks.get(1));  // else endblock
                blocks.get(1).setLabeled(true);
            }
            curBasicBlock.setTerminator(branch);
            tureBlock.setLabeled(true);
            if (i != lAndExp.getEqExps().size() - 1) {
                curBasicBlock = tureBlock;
            }
        }
        return value;
    }

    public Value visitEqExp(EqExp eqExp) {
        if (eqExp.getRelExps().size() == 1) {
            return visitRelExp(eqExp.getRelExps().get(0));
        }
        Value res = null;
        for (int i = 1; i < eqExp.getRelExps().size(); i++) {
            Value value1 = i == 1 ? visitRelExp(eqExp.getRelExps().get(0)) : res;
            Value value2 = visitRelExp(eqExp.getRelExps().get(i));
            // 如果value1 和 value2 是Const
            res = getCmpInstr(value1, value2, Compare.CondType.getOp(eqExp.getOps().get(i - 1).getContent()));
        }
        return res;
    }

    public Value visitRelExp(RelExp relExp) {
        if (relExp.getAddExps().size() == 1) {
            return visitAddExp(relExp.getAddExps().get(0));
        }
        Value res = null;
        for (int i = 1; i < relExp.getAddExps().size(); i++) {
            Value value1 = i == 1 ? visitAddExp(relExp.getAddExps().get(0)) : res;
            Value value2 = visitAddExp(relExp.getAddExps().get(i));
            // 如果value1 和 value2 是Const
            res = getCmpInstr(value1, value2, Compare.CondType.getOp(relExp.getOps().get(i - 1).getContent()));
        }
        return res;
    }

    public Value getCmpInstr(Value value1, Value value2, Compare.CondType type) {
        if (value1 == null || value2 == null) {
            return null;
        }
        if (value1 instanceof Constant && value2 instanceof Constant) {
            return cmpConst((Constant) value1, (Constant) value2, type);
        } else {
            Value zextValue1 = zext(value1);
            Value zextValue2 = zext(value2);
            Compare compare = new Compare("", type);
            compare.addOperands(zextValue1);
            compare.addOperands(zextValue2);
            curBasicBlock.appendInstr(compare, true);
            return compare;
        }
    }

    public Constant cmpConst(Constant value1, Constant value2, Compare.CondType type) {
        ValueType.Type valueType = new ValueType.Type(Integer1Ty);
        switch (type) {
            case EQ:
                return new Constant(valueType, Integer.parseInt(value1.getName())
                        == Integer.parseInt(value2.getName()) ? "1" : "0");
            case NE:
                return new Constant(valueType, Integer.parseInt(value1.getName())
                        != Integer.parseInt(value2.getName()) ? "1" : "0");
            case SGE:
                return new Constant(valueType, Integer.parseInt(value1.getName())
                        >= Integer.parseInt(value2.getName()) ? "1" : "0");
            case SGT:
                return new Constant(valueType, Integer.parseInt(value1.getName())
                        > Integer.parseInt(value2.getName()) ? "1" : "0");
            case SLE:
                return new Constant(valueType, Integer.parseInt(value1.getName())
                        <= Integer.parseInt(value2.getName()) ? "1" : "0");
            case SLT:
                return new Constant(valueType, Integer.parseInt(value1.getName())
                    < Integer.parseInt(value2.getName()) ? "1" : "0");
            default:
                return null;
        }
    }

    public Value getBinInstr(Value value1, Value value2, Type type) {
        if (value1 == null || value2 == null) {
            return null;
        }
        if (value1 instanceof Constant && value2 instanceof Constant) {
            return calConst((Constant) value1, (Constant) value2, type);
        } else {
            Value zextValue1 = zext(value1);
            Value zextValue2 = zext(value2);
            BinaryOperator binOp = new BinaryOperator(new ValueType.Type(Integer32Ty), type, "");
            binOp.addOperands(zextValue1);
            binOp.addOperands(zextValue2);
            curBasicBlock.appendInstr(binOp, true);
            return binOp;
        }
    }

    public Constant calConst(Constant value1, Constant value2, Type type) {
        switch (type) {
            case ADD:
                return new Constant(Integer.toString(Integer.parseInt(value1.getName())
                        + Integer.parseInt(value2.getName())));
            case SUB:
                return new Constant(Integer.toString(Integer.parseInt(value1.getName())
                        - Integer.parseInt(value2.getName())));
            case MUL:
                return new Constant(Integer.toString(Integer.parseInt(value1.getName())
                        * Integer.parseInt(value2.getName())));
            case SDIV:
                return new Constant(Integer.toString(Integer.parseInt(value1.getName())
                        / Integer.parseInt(value2.getName())));
            case SREM:
                return new Constant(Integer.toString(Integer.parseInt(value1.getName())
                        % Integer.parseInt(value2.getName())));
            default:
                return null;
        }
    }

    public Value zext(Value value) {
        if (value instanceof Constant) {
            Constant constant = ((Constant) value).deepClone();
            constant.setTp(new ValueType.Type(Integer32Ty));
            return constant;
        }
        if (value.getTp().getDataType() != Integer32Ty) {
            Zext zext = new Zext(new ValueType.Type(Integer32Ty), "");
            zext.addOperands(value);
            curBasicBlock.appendInstr(zext, true);
            return zext;
        }
        return value;
    }

    public Value trunc(Value value) {
        if (value instanceof Constant) {
            Constant constant = ((Constant) value).deepClone();
            constant.setTp(new ValueType.Type(Integer8Ty));
            return constant;
        }
        if (value.getTp().getDataType() != Integer8Ty) {
            Trunc trunc = new Trunc(new ValueType.Type(Integer8Ty), "");
            trunc.addOperands(value);
            curBasicBlock.appendInstr(trunc, true);
            return trunc;
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
            Symbol symbol;
            if ((symbol = curTable.findSym(name, "Func")) == null) {
                errors.add(new Error("c", unaryExp.getIdent().getLineno()));
                return null;
            }
            Function callFunc = (Function) symbol.getValue();
            Call call = new Call(callFunc);
            if (unaryExp.hasFuncRParams()) {
                if (unaryExp.getArgc() != ((Function) symbol.getValue()).getArgc()) {
                    errors.add(new Error("d", unaryExp.getLineno()));
                } else {
                    call.setFuncRParams(visitFuncRParams(symbol.getFuncFParams(), unaryExp.getFuncRParams()));
                }
            }
            curBasicBlock.appendInstr(call, callFunc.isNotVoid());
            return call;
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
                Compare compare = new Compare("", Compare.CondType.getOp("=="));
                compare.addOperands(value1);
                compare.addOperands(value2);
                curBasicBlock.appendInstr(compare, true);
                return compare;
            }
        }
    }

    public Value visitPrimaryExp(PrimaryExp primaryExp) {
        AstNode node = primaryExp.getPrimaryExp();
        if (node instanceof Exp) {
            return visitAddExp(((Exp) node).getAddExp());
        } else if (node instanceof LVal) {
            return visitLVal((LVal) node, true);
        } else if (node instanceof Number) {
            return new Constant(((Number) node).getNumber());
        } else if (node instanceof Character) {
            return new Constant(new ValueType.Type(Integer8Ty), ((Character) node).getChar());
        }
        return null;
    }

    public Value visitLVal(LVal lVal, boolean isOperand) {
        String name = lVal.getIdentName();
        Symbol symbol;
        if ((symbol = curTable.findSym(name, "Var")) == null) {
            errors.add(new Error("c", lVal.getIdent().getLineno()));
            return new Value(new ValueType.Type(Integer32Ty), "null");
        }
        Value value = symbol.getValue();
        // 如果是左值，只能是非Const类型的，并且要的是地址
        if (isOperand) {
            return visitOperand(lVal, value);
        } else {
            return visitLeft(lVal, value);
        }
    }

    /**
     * @param lVal  ast中的左值结点
     * @param value 左值
     *              只能是非Const类型的，返回类型是地址
     * @return 左值地址
     */
    public Value visitLeft(LVal lVal, Value value) {
        Value res = value;
        if (lVal.isArrayElement()) { // 数组元素
            Value index = visitAddExp(lVal.getExp().getAddExp());
            res = getElement(value, index);
        }
        return res;
    }

    /**
     * @param lVal ast语法书中的左值节点
     * @param value 操作数
     * @return 操作数的值
     */
    public Value visitOperand(LVal lVal, Value value) {
        Value operand = value;
        // 包含以下情况：
        // 1. 全局：常量（包括数组和数组元素），变量
        // 2. 局部：常量 (包括数组和数组元素)，变量
        // 3. 其中常量数组元素下标有确定和不确定两种情况（若确定可以直接获取值，不确定则需要计算偏移地址）
        if (value instanceof GlobalVariable && ((GlobalVariable) value).isConstant()) {
            // 获取全局常量和常量数组元素的值
            GlobalVariable globalVar = (GlobalVariable) value;
            if (!globalVar.isArray()) { // 常量
                return ((Constant) globalVar.getInitVal().get(0)).deepClone();
            } else if (globalVar.isArray() && lVal.isArrayElement()) {  // 数组元素
                Value index = visitAddExp(lVal.getExp().getAddExp());
                if (index instanceof Constant) {    // 下标是常数
                    int bis = Integer.parseInt(index.getName());
                    return ((Constant) globalVar.getInit(bis)).deepClone();
                }  else {    // 下标不确定
                    operand = getElement(value, index); // 获取地址
                }
            }
        }
        if (value instanceof Alloca && ((Alloca) value).isConst()) {
            // 获取局部常量和常量数组元素的值
            Alloca alloca = (Alloca) value;
            if (!alloca.isArray()) {
                return ((Constant) alloca.getConstInits().get(0)).deepClone();
            } else if (alloca.isArray() && lVal.isArrayElement()) { // 数组元素
                Value index = visitAddExp(lVal.getExp().getAddExp());
                if (index instanceof Constant) {    // 下标确定
                    int bis = Integer.parseInt(index.getName());
                    return ((Constant) alloca.getInit(bis)).deepClone();
                } else {    // 下标不确定
                    operand = getElement(value, index);
                }
            }
        }
        // 全局和局部数组，需要传递数组的指针
        if (!lVal.isArrayElement() && value.getTp() instanceof ValueType.PointerType
                && value.getTp().getInnerType() instanceof ValueType.ArrayType) {
            GetElementPtr getElementPtr = new GetElementPtr(value.getTp(), "");
            getElementPtr.addOperands(value);
            getElementPtr.addOperands(new Constant("0"));
            getElementPtr.addOperands(new Constant("0"));
            curBasicBlock.appendInstr(getElementPtr, true);
            operand = getElementPtr;
            return operand;
        }
        if (lVal.isArrayElement()) { // 数组元素
            Value index = visitAddExp(lVal.getExp().getAddExp());
            operand = getElement(value, index);
        }
        // 全局和局部变量，以及数组元素指针：直接load
        if (operand instanceof GlobalVariable
                || operand instanceof Alloca
                || operand instanceof GetElementPtr) {
            Load load = new Load(operand.getTp().getInnerType(), "");
            load.addOperands(operand);
            curBasicBlock.appendInstr(load, true);
            return load;
        }
        return operand;
    }

    public Value getElement(Value value, Value index) {
        Value operand = value;
        if (value.getTp().getInnerType() instanceof ValueType.PointerType) {
            // 如果是数组指针，那么需要先load出数组地址，再用load出的地址寻找元素
            Load load = new Load(value.getTp().getInnerType(), "");
            load.addOperands(value);
            curBasicBlock.appendInstr(load, true);
            operand = load;
        }
        GetElementPtr getElementPtr = new GetElementPtr(operand.getTp(), "");
        getElementPtr.addOperands(operand);
        if (operand.getTp().getInnerType() instanceof ValueType.ArrayType) {
            // 如果是数组指针则需要索引添加0
            getElementPtr.addOperands(new Constant("0"));
        }
        getElementPtr.addOperands(index); // TODO: 有变化 原本为bis
        curBasicBlock.appendInstr(getElementPtr, true);
        operand = getElementPtr;
        return operand;
    }

    public ArrayList<Value> visitFuncRParams(FuncFParams ffp, FuncRParams frp) {
        ArrayList<Value> params = new ArrayList<>();
        if (ffp.getArgc() != frp.getArgc()) {
            return params;
        }
        for (int i = 0; i < ffp.getArgc(); i++) {
            FuncFParam fp = ffp.getParam(i);
            Exp rp = frp.getParam(i);
            boolean isArray = false;    //  是否为数组
            String type = "";        //  类型为what, 只考虑数组情况。
            String name = rp.getIdentName();    //获取符号
            Symbol sym;
            if ((sym = curTable.findSym(name, "Var")) != null) {
                SymType st = sym.getType();
                isArray = sym.getType().isArray() && !rp.isArray();
                type = st.getType().toLowerCase();
            }
            if (isArray != fp.isArray()) {  // array or var不匹配
                errors.add(new Error("e", rp.getLineno()));
                return params;
            } else if (isArray && fp.isArray()) {    // char or int 不匹配
                if (!type.equals(fp.getType().getContent())) {
                    errors.add(new Error("e", rp.getLineno()));
                    return params;
                }
            }
            Value value = visitAddExp(rp.getAddExp()); // TODO: 类型匹配
            if (!isArray && fp.getType().getContent().equals("int")) {
                value = zext(value);
            } else if (!isArray && fp.getType().getContent().equals("char")) {
                value = trunc(value);
            }
            params.add(value);
        }
        return params;
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

    public ValueType.Type getDataType(String type) {
        switch (type) {
            case "Int":
                return new ValueType.Type(Integer32Ty);
            case "Char":
                return new ValueType.Type(Integer8Ty);
            case "IntArray":
                return new ValueType.ArrayType(Integer32Ty);
            case "CharArray":
                return new ValueType.ArrayType(Integer8Ty);
            case "IntPointer":
                return new ValueType.PointerType(Integer32Ty);
            case "CharPointer":
                return new ValueType.PointerType(Integer8Ty);
            default:
                return new ValueType.Type(VoidTy);
        }
    }
}
