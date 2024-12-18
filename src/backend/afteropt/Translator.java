package backend.afteropt;

import backend.beforeopt.RegManager;
import backend.mips.*;
import backend.mips.StackManager;
import llvmir.Module;
import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.*;
import llvmir.values.instr.*;

import java.util.*;

import static backend.mips.MipsInstrType.*;

/**
 * 后端分为两步：第一步直接将llvm转化成mips,仍使用虚拟寄存器；
 * 第二步将虚拟寄存器映射到物理寄存器上
 * Translator类用来实现第一步，核心点在于遵循mips规范的基础上使用无限个虚拟寄存器
 */
public class Translator {
    private final Module module;
    private final MipsModule mipsModule;
    private final StackManager stackManager = StackManager.getInstance();
    private final RegManager regManager = new RegManager();
    private MipsFunction currentFunction;
    private Function irFunction;
    private BasicBlock curBlock;
    private int maxParams = 0;

    public Translator(Module module) {
        this.module = module;
        mipsModule = new MipsModule();
    }

    public MipsModule getMipsModule() {
        return mipsModule;
    }

    public void genMipsCode() {
        genDataSegment();
        genTextSegment();
    }

    public void genDataSegment() {
        LinkedList<GlobalVariable> globalVariables = module.getGlobalValues();
        for (GlobalVariable globalVar: globalVariables) {
            String name = globalVar.getName();
            String dataType = getDataType(globalVar.getTp().getDataType(), globalVar.isString());
            String init = globalVar.getInitValtoString();
            MipsData data = new MipsData(name, dataType, init);
            mipsModule.addGlobalData(data);
            stackManager.addGlobalData(globalVar.getFullName());
        }
    }

    public void genTextSegment() {
        ArrayList<Function> functions = module.getFunctions();
        calMaxParam(functions);
        Collections.reverse(functions); // 翻转list,先解析main
        for (Function function: functions) {
            irFunction = function;
            currentFunction = new MipsFunction(function.getName());
            mipsModule.addFunction(currentFunction);
            genFunction(function);
        }
        HashMap<String, Function> declares = module.getDeclares();
        for (Function declare: declares.values()) {
            genDeclare(declare);
        }
    }

    public void calMaxParam(ArrayList<Function> functions) {
        for (Function function: functions) {
            int num = function.getArgc();
            if (num > maxParams) {
                maxParams = num;
            }
        }
    }

    public void genDeclare(Function declare) {
        // 添加函数标签
        // MipsInstruction label = new MipsInstruction(declare.getName());
        MipsFunction mipsFunction = new MipsFunction(declare.getName());
        mipsModule.addFunction(mipsFunction);
        if (declare.getName().equals("putint")) {
            mipsFunction.addInstr(new MipsInstruction(LI, "$v0", "1"));
        }
        if (declare.getName().equals("putch")) {
            mipsFunction.addInstr(new MipsInstruction(LI, "$v0", "11"));
        }
        if (declare.getName().equals("putstr")) {
            mipsFunction.addInstr(new MipsInstruction(LI, "$v0", "4"));
        }
        if (declare.getName().equals("getint")) {
            mipsFunction.addInstr(new MipsInstruction(LI, "$v0", "5"));
        }
        if (declare.getName().equals("getchar")) {
            mipsFunction.addInstr(new MipsInstruction(LI, "$v0", "12"));
        }
        mipsFunction.addInstr(new MipsInstruction(SYSCALL, ""));
        mipsFunction.addInstr(new MipsInstruction(JR, "$ra"));
    }

    public void genFunction(Function function) {
        // 函数序言
        allocStackFrame(function);
        for (BasicBlock basicBlock: function.getBasicBlocks()) {
            curBlock = basicBlock;
            genBasicBlock(basicBlock);
        }
        // 函数尾声
        stackManager.clear();
    }

    /**
     * @param function 被调用的函数
     * <p>
     *                 函数被调用后申请一个函数栈帧，以此压入ra，fp，局部变量，保存寄存器，参数<br>
     *                 ra: 若函数非叶子函数，则需要将ra的值压入栈<br>
     *                 fp: 调用者的栈顶，即旧的sp<br>
     *                 局部变量: 函数使用的局部变量<br>
     *                 保存寄存器: s0->s7<br>
     *                 参数: 如果函数非叶子函数，则留给子函数将参数的值存在此处
     * </p>
     */
    public void allocStackFrame(Function function) {
        int size = maxParams * 4;
        // 从栈顶到栈底记录映射，依次为 argument, save reg， local var, fp, ra
        // 记录参数映射
        allocArguments(function);
        // 记录save reg映射
        for (int i = 0; i < 18; i++) {
            if (i < 10) {
                stackManager.putVirtualReg("$t" + i, 4);
            } else {
                stackManager.putVirtualReg("$s" + (i - 10), 4);
            }
            size += 4;
        }
        // 保存local var: MIPS在保存局部变量时，遵循的顺序是：先定义的变量后入栈
        size += saveLocalVariables(function);
        // 保存fp, ra
        size += 8;
        // 更新sp, fp寄存器:每个函数栈内部的fp都指向了调用者函数的栈顶
        currentFunction.addInstr(new MipsInstruction(MOVE, "$fp", "$sp"));
        currentFunction.addInstr(new MipsInstruction(ADDIU, "$sp", "$sp", "-" + size));
        // 将fp存入栈空间
        int ptrFp = stackManager.putVirtualReg("$fp", 4);
        // 存入ra（暂时不管是否为叶子函数）
        int ptrRa = stackManager.putVirtualReg("$ra", 4);
        currentFunction.addInstr(new MipsInstruction(SW, "$ra", "$sp", String.valueOf(ptrRa)));
        currentFunction.addInstr(new MipsInstruction(SW, "$fp", "$sp", String.valueOf(ptrFp)));
        // 调用者的传参在栈中位置构建字典
        for (int i = 0; i < function.getArgc(); i++) {
            if (i > 3) {
                int ptr = stackManager.putVirtualReg(function.getFuncFParams().get(i).getFullName(), 4);
                if (irFunction.getGlobalRegsMap().containsKey(function.getFuncFParams().get(i))) {
                    int reg = function.getGlobalRegsMap().get(function.getFuncFParams().get(i));
                    MipsRegister phyReg = regManager.getReg(reg);
                    currentFunction.addInstr(new MipsInstruction(LW, phyReg.getName(), "$sp", String.valueOf(ptr)));
                }
            } else {
                if (irFunction.getGlobalRegsMap().containsKey(function.getFuncFParams().get(i))) {
                    int reg = function.getGlobalRegsMap().get(function.getFuncFParams().get(i));
                    MipsRegister phyReg = regManager.getReg(reg);
                    currentFunction.addInstr(new MipsInstruction(MOVE, phyReg.getName(), "$a" + i));
                }
                stackManager.addPtr(4);
            }
        }
    }

    public void allocArguments(Function function) {
        // 记录argument->ptr映射
        for (int i = 0; i < maxParams; i++) {
            stackManager.setVirtualReg("$param" + i, -4 * (maxParams - i)); // TODO
        }
    }

    public int saveLocalVariables(Function function) {
        int localVarSize = 0;
        for (BasicBlock basicBlock: function.getBasicBlocks()) {
            for (Instruction instruction: basicBlock.getInstructions()) {
                if (instruction instanceof Alloca) {
                    // 无论是int还是char都分配4字节，数组则分配4*size
                    Alloca alloca = (Alloca) instruction;
                    int size = 4;
                    if (alloca.getDim() > 0) {
                        size = 4 * alloca.getDim();
                    }
                    localVarSize += size;
                    // 将local Var在栈空间的位置确定，暂时不填值
                    stackManager.putVirtualReg(alloca.getFullName(), size);
                } else if (irFunction.getValueInStack().contains(instruction.def())) {
                    localVarSize += 4;
                    stackManager.putVirtualReg(instruction.def().getFullName(), 4);
                } else if (instruction instanceof Move && !(((Move) instruction).getDst() instanceof Instruction)) {
                    localVarSize += 4;
                    stackManager.putVirtualReg(((Move) instruction).getDst().getFullName(), 4);
                    irFunction.getValueInStack().add(((Move) instruction).getDst());
                }
            }
        }
        return localVarSize;
    }

    public void genBasicBlock(BasicBlock basicBlock) {
        if (basicBlock.isLabeled()) {
            currentFunction.addInstr(new MipsInstruction(basicBlock.getLabel()));
        }
        boolean isMain = basicBlock.getParent().getName().equals("main");
        for (Instruction instruction: basicBlock.getInstructions()) {
            genInstruction(instruction, isMain);
        }
    }

    public void genInstruction(Instruction instruction, boolean isMain) {
        if (instruction instanceof Store) {
            genStore((Store) instruction);
        } else if (instruction instanceof Load) {
            genLoad((Load) instruction);
        } else if (instruction instanceof Return) {
            genReturn((Return) instruction, isMain);
        } else if (instruction instanceof Call) {
            genCall((Call) instruction);
        } else if (instruction instanceof BinaryOperator) {
            genBinaryOperator((BinaryOperator) instruction);
        } else if (instruction instanceof Zext) {
            genZext((Zext) instruction);
        } else if (instruction instanceof Trunc) {
            genTrunc((Trunc) instruction);
        } else if (instruction instanceof Alloca) {
            currentFunction.addInstr(new MipsInstruction("# " + instruction));
        } else if (instruction instanceof GetElementPtr) {
            genElementPtr((GetElementPtr) instruction);
        } else if (instruction instanceof Compare) {
            currentFunction.addInstr(new MipsInstruction("# " + instruction));
            genCompare((Compare) instruction);
        } else if (instruction instanceof Branch) {
            genBranch((Branch) instruction);
        } else if (instruction instanceof Move) {
            genMove((Move) instruction);
        }
    }

    public void genMove(Move move) {
        currentFunction.addInstr(new MipsInstruction("# " + move));
        Value dst = move.getDst();
        Value src = move.getSrc();
        MipsRegister srcReg = getReg(src);
        if (src instanceof Constant && !src.getName().equals("0")) {
            currentFunction.addInstr(new MipsInstruction(LI, srcReg.getName(), src.getName()));
        }
        if (dst instanceof Phi) {
            saveInStack(dst, srcReg);
            return;
        }
        MipsRegister dstReg = getReg(dst);
        if (!dstReg.getName().equals(srcReg.getName())) {
            currentFunction.addInstr(new MipsInstruction(MOVE, dstReg.getName(), srcReg.getName()));
        }
        saveInStack(dst, dstReg);
    }

    public void genStore(Store store) {
        currentFunction.addInstr(new MipsInstruction("# " + store.toString()));
        Value value = store.getOperands().get(0);
        Value addr = store.getOperands().get(1);
        MipsRegister reg = getReg(value);
        if (value instanceof Constant && !value.getName().equals("0")) {
            currentFunction.addInstr(new MipsInstruction(LI, reg.getName(), value.getName()));
        }
        if (stackManager.isGlobalData(addr.getFullName())) {    // 全局变量
            if (value.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                currentFunction.addInstr(new MipsInstruction(SB, reg.getName(), addr.getName()));
            } else {
                currentFunction.addInstr(new MipsInstruction(SW, reg.getName(), addr.getName()));
            }
        } else {
            MipsRegister temp0 = getReg(addr);
            if (value.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                currentFunction.addInstr(new MipsInstruction(SB, reg.getName(), temp0.getName(), "0"));
            } else {
                currentFunction.addInstr(new MipsInstruction(SW, reg.getName(), temp0.getName(), "0"));
            }
        }
    }

    public void genLoad(Load load) {
        currentFunction.addInstr(new MipsInstruction("# " + load.toString()));
        Value addr = load.getOperands().get(0);
        MipsRegister temp = getReg(load); // load到临时寄存器
        if (stackManager.isGlobalData(addr.getFullName())) {    // 全局变量
            // currentFunction.addInstr(new MipsInstruction(LA, temp.getName(), addr.getName()));
            if (load.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                currentFunction.addInstr(new MipsInstruction(LB, temp.getName(), addr.getName()));
            } else {
                currentFunction.addInstr(new MipsInstruction(LW, temp.getName(), addr.getName()));
            }
        } else {
            MipsRegister temp0 = getReg(addr);
            if (load.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                currentFunction.addInstr(new MipsInstruction(LB, temp.getName(), temp0.getName(), "0"));
            } else {
                currentFunction.addInstr(new MipsInstruction(LW, temp.getName(), temp0.getName(), "0"));
            }
        }
        saveInStack(load, temp);
    }

    public void genReturn(Return ret, boolean isMain) {
        currentFunction.addInstr(new MipsInstruction("# " + ret.toString()));
        // 设置返回值
        if (!ret.getOperands().isEmpty()) {
            Value value = ret.getOperands().get(0);
            if (value instanceof Constant) {
                currentFunction.addInstr(new MipsInstruction(ADDIU, "$v0", "$zero", value.getName()));
            } else {
                MipsRegister reg = getReg(value);
                currentFunction.addInstr(new MipsInstruction(ADDU, "$v0", "$zero", reg.getName()));
            }
        }
        // 恢复ra
        int raPtr = stackManager.getVirtualPtr("$ra");
        currentFunction.addInstr(new MipsInstruction(LW, "$ra", "$sp", String.valueOf(raPtr)));
        // 恢复栈帧
        int ptr = stackManager.getVirtualPtr("$fp");
        currentFunction.addInstr(new MipsInstruction(LW, "$fp", "$sp", String.valueOf(ptr)));
        currentFunction.addInstr(new MipsInstruction(MOVE, "$sp", "$fp"));
        // 返回调用者
        if (isMain) {
            currentFunction.addInstr(new MipsInstruction(LI, "$v0", String.valueOf(10)));
            currentFunction.addInstr(new MipsInstruction(SYSCALL));
        } else {
            currentFunction.addInstr(new MipsInstruction(JR, "$ra"));
            // currentFunction.addInstr(new MipsInstruction(NOP));
        }
    }

    public void genCall(Call call) {
        currentFunction.addInstr(new MipsInstruction("# " + call.toString()));
        Function function = call.getCallFunc();
        if (function.getName().equals("putstr")) {
            GetElementPtr getElementPtr = (GetElementPtr) call.getFuncRParams().get(0);
            currentFunction.addInstr(new MipsInstruction(LA, "$a0", getElementPtr.getOperands().get(0).getName()));
            // 跳转到目标函数
            currentFunction.addInstr(new MipsInstruction(JAL, function.getName()));
            return;
        }
        // 保存现场
        HashMap<Integer, Value> unused = call.getSaveMap();
        for (Integer reg: unused.keySet()) {
            MipsRegister tempReg = regManager.getReg(reg);
            int ptr = stackManager.getVirtualPtr(tempReg.getName());
            currentFunction.addInstr(new MipsInstruction(SW, tempReg.getName(), "$sp", String.valueOf(ptr)));
        }
        // 参数传递
        ArrayList<Value> funcRParams = call.getFuncRParams();
        for (int i = 0; i < funcRParams.size(); i++) {
            Value value = funcRParams.get(i);
            if (i < 4) {
                if (value instanceof Constant) {
                    String constVar = value.getName();
                    currentFunction.addInstr(new MipsInstruction(LI, "$a" + i, constVar));
                } else {
                    MipsRegister argue = getReg(value);
                    currentFunction.addInstr(new MipsInstruction(ADDU, "$a" + i, "$zero", argue.getName()));
                }
            } else {
                int ptr = stackManager.getVirtualPtr("$param" + i);
                MipsRegister temp = getReg(value);
                if (value instanceof Constant && !value.getName().equals("0")) {
                    currentFunction.addInstr(new MipsInstruction(LI, temp.getName(), value.getName()));
                }
                currentFunction.addInstr(new MipsInstruction(SW, temp.getName(), "$sp", String.valueOf(ptr)));
            }
        }
        // 跳转到目标函数
        currentFunction.addInstr(new MipsInstruction(JAL, function.getName()));
        // currentFunction.addInstr(new MipsInstruction(NOP));
        // 恢复现场
        for (Integer reg: unused.keySet()) {
            MipsRegister temp = regManager.getReg(reg);
            int ptr = stackManager.getVirtualPtr(temp.getName());
            currentFunction.addInstr(new MipsInstruction(LW,temp.getName(), "$sp", String.valueOf(ptr)));
        }
        // 回到调用者
        if (function.isNotVoid()) {
            MipsRegister ret = getReg(call);
            currentFunction.addInstr(new MipsInstruction(MOVE, ret.getName(), "$v0"));
            saveInStack(call, ret);
        }
    }

    public void genBinaryOperator(BinaryOperator binaryOperator) {
        currentFunction.addInstr(new MipsInstruction("# " + binaryOperator.toString()));
        switch (binaryOperator.getIrType()) {
            case ADD:
                genAddInstr(binaryOperator);
                break;
            case SUB:
                genSubInstr(binaryOperator);
                break;
            case MUL:
            case SDIV:
            case SREM:
                genHiLoInst(binaryOperator, binaryOperator.getIrType());
                break;
            default:
                break;
        }
    }

    public void genAddInstr(BinaryOperator binaryOperator) {
        Value operand1 = binaryOperator.getOperands().get(0);
        Value operand2 = binaryOperator.getOperands().get(1);
        MipsRegister temp = getReg(binaryOperator);
        if (operand1 instanceof Constant && operand2 instanceof Constant) {
            int ans = Integer.parseInt(operand1.getName()) + Integer.parseInt(operand2.getName());
            currentFunction.addInstr(new MipsInstruction(ADDIU, temp.getName(), "$zero", String.valueOf(ans)));
        } else {
            if (operand1 instanceof Constant) {
                MipsRegister reg = getReg(operand2);
                currentFunction.addInstr(new MipsInstruction(ADDIU, temp.getName(), reg.getName(), operand1.getName()));
            } else if (operand2 instanceof Constant) {
                MipsRegister reg = getReg(operand1);
                currentFunction.addInstr(new MipsInstruction(ADDIU, temp.getName(), reg.getName(), operand2.getName()));
            } else {
                MipsRegister op1 = getReg(operand1);
                MipsRegister op2 = getReg(operand2);
                currentFunction.addInstr(new MipsInstruction(ADDU, temp.getName(), op1.getName(), op2.getName()));
            }
        }
        saveInStack(binaryOperator, temp);
    }

    public void genSubInstr(BinaryOperator binaryOperator) {
        Value operand1 = binaryOperator.getOperands().get(0);
        Value operand2 = binaryOperator.getOperands().get(1);
        if (operand1 instanceof Constant && operand2 instanceof Constant) {
            MipsRegister temp = getReg(binaryOperator);
            int ans = Integer.parseInt(operand1.getName()) - Integer.parseInt(operand2.getName());
            currentFunction.addInstr(new MipsInstruction(ADDIU, temp.getName(), "$zero", String.valueOf(ans)));
            saveInStack(binaryOperator, temp);
        } else {
            MipsRegister temp = getReg(binaryOperator);
            if (operand1 instanceof Constant) {
                // 第一个操作数是常数，需要addi $t9, $zero, op1, 再subu $t2, $t9, $t1
                MipsRegister temp0 = getReg(operand1);
                if (!operand1.getName().equals("0")) {
                    currentFunction.addInstr(new MipsInstruction(ADDIU, temp0.getName(), "$zero", operand1.getName()));
                }
                MipsRegister reg = getReg(operand2);
                currentFunction.addInstr(new MipsInstruction(SUBU, temp.getName(), temp0.getName(), reg.getName()));
            } else if (operand2 instanceof Constant) {
                MipsRegister reg = getReg(operand1);
                currentFunction.addInstr(new MipsInstruction(SUBI, temp.getName(), reg.getName(), operand2.getName()));
            } else {
                MipsRegister op1 = getReg(operand1);
                MipsRegister op2 = getReg(operand2);
                currentFunction.addInstr(new MipsInstruction(SUBU, temp.getName(), op1.getName(), op2.getName()));
            }
            saveInStack(binaryOperator, temp);
        }

    }

    public void genHiLoInst(BinaryOperator binaryOperator, Instruction.Type op) {
        Value operand1 = binaryOperator.getOperands().get(0);
        Value operand2 = binaryOperator.getOperands().get(1);
        if (operand1 instanceof Constant && operand2 instanceof Constant) {
            int ans;
            switch (op) {
                case MUL:
                    ans = Integer.parseInt(operand1.getName()) * Integer.parseInt(operand2.getName());
                    break;
                case SDIV:
                    ans = Integer.parseInt(operand1.getName()) / Integer.parseInt(operand2.getName());
                    break;
                case SREM:
                    ans = Integer.parseInt(operand1.getName()) % Integer.parseInt(operand2.getName());
                    break;
                default:
                    ans = 0;
            }
            MipsRegister temp = getReg(binaryOperator);
            currentFunction.addInstr(new MipsInstruction(ADDIU, temp.getName(), "$zero", String.valueOf(ans)));
            saveInStack(binaryOperator, temp);
            return;
        }
        MipsRegister op1 = getReg(operand1);
        MipsRegister op2 = getReg(operand2);
        if (operand1 instanceof Constant && !operand1.getName().equals("0")) {
            currentFunction.addInstr(new MipsInstruction(ADDIU, op1.getName(), "$zero", operand1.getName()));
        }
        if (operand2 instanceof Constant && !operand2.getName().equals("0")) {
            currentFunction.addInstr(new MipsInstruction(ADDIU, op2.getName(), "$zero", operand2.getName()));
        }
        MipsInstrType type = op == Instruction.Type.MUL ? MULT : DIV;
        currentFunction.addInstr(new MipsInstruction(type, op1.getName(), op2.getName()));
        MipsRegister temp = getReg(binaryOperator);
        if (op == Instruction.Type.MUL || op == Instruction.Type.SDIV) {
            currentFunction.addInstr(new MipsInstruction(MFLO, temp.getName()));
        } else {
            currentFunction.addInstr(new MipsInstruction(MFHI, temp.getName()));
        }
        saveInStack(binaryOperator, temp);
    }

    public void genTrunc(Trunc trunc) {
        currentFunction.addInstr(new MipsInstruction("# " + trunc));
        Value op1 = trunc.getOperands().get(0);
        MipsRegister temp = getReg(trunc);
        MipsRegister reg = getReg(op1);
        if (op1 instanceof Constant && !op1.getName().equals("0")) {
            currentFunction.addInstr(new MipsInstruction(LI,reg.getName(), op1.getName()));
        }
        currentFunction.addInstr(new MipsInstruction(ANDI, temp.getName(), reg.getName(), "0xFF"));
        saveInStack(trunc, temp);
    }

    public void genZext(Zext zext) {
        currentFunction.addInstr(new MipsInstruction("# " + zext));
        Value op1 = zext.getOperands().get(0);
        MipsRegister temp = getReg(zext);
        MipsRegister reg = getReg(op1);
        if (op1 instanceof Constant && !op1.getName().equals("0")) {
            currentFunction.addInstr(new MipsInstruction(LI,reg.getName(), op1.getName()));
        }
        currentFunction.addInstr(new MipsInstruction(ANDI, temp.getName(), reg.getName(), "0xFF"));
        saveInStack(zext,temp);
    }

    public void genElementPtr(GetElementPtr getElementPtr) {    // 计算地址
        currentFunction.addInstr(new MipsInstruction("# " + getElementPtr.toString()));
        Value firstAddr = getElementPtr.getOperands().get(0);
        int firstPtr = stackManager.getVirtualPtr(firstAddr.getFullName());
        Value bis1 = getElementPtr.getOperands().get(1);
        MipsRegister ptrReg = getReg(getElementPtr);;
        // 获取首地址
        if (firstPtr >= 0 && firstAddr instanceof Alloca) {    // 可能为数组
            currentFunction.addInstr(new MipsInstruction(ADDIU, ptrReg.getName(), "$sp", String.valueOf(firstPtr)));
        } else {
            if (stackManager.isGlobalData(firstAddr.getFullName())) {
                currentFunction.addInstr(new MipsInstruction(LA, ptrReg.getName(), firstAddr.getName()));
            } else {
                MipsRegister temp = getReg(firstAddr);
                currentFunction.addInstr(new MipsInstruction(ADDU, ptrReg.getName(), "$zero", temp.getName()));
            }
        }
        // 加上第一部分索引
        if (bis1 instanceof Constant) {
            int ptr = Integer.parseInt(bis1.getName()) * 4;
            if (firstAddr.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                ptr = Integer.parseInt(bis1.getName());
            }
            if (ptr != 0) {
                currentFunction.addInstr(new MipsInstruction(ADDIU, ptrReg.getName(), ptrReg.getName(), String.valueOf(ptr)));
            }
        } else {
            MipsRegister temp = getReg(bis1);
            if (firstAddr.getTp().getDataType() == ValueType.DataType.Integer32Ty) {
                currentFunction.addInstr(new MipsInstruction(SLL, "$v1", temp.getName(), String.valueOf(2)));
                currentFunction.addInstr(new MipsInstruction(ADDU, ptrReg.getName(), ptrReg.getName(), "$v1"));
            } else {
                currentFunction.addInstr(new MipsInstruction(ADDU, ptrReg.getName(), ptrReg.getName(), temp.getName()));
            }

        }
        if (getElementPtr.getOperands().size() > 2) {
            Value bis2 = getElementPtr.getOperands().get(2);
            if (bis2 instanceof Constant) {
                int ptr = Integer.parseInt(bis2.getName()) * 4;
                if (firstAddr.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                    ptr = Integer.parseInt(bis2.getName());
                }
                if (ptr != 0) {
                    currentFunction.addInstr(new MipsInstruction(ADDIU, ptrReg.getName(), ptrReg.getName(), String.valueOf(ptr)));
                }
            } else {
                MipsRegister temp = getReg(bis2);
                if (firstAddr.getTp().getDataType() == ValueType.DataType.Integer32Ty) {
                    currentFunction.addInstr(new MipsInstruction(SLL, "$v1", temp.getName(), String.valueOf(2)));
                    currentFunction.addInstr(new MipsInstruction(ADDU, ptrReg.getName(), ptrReg.getName(), "$v1"));
                } else {
                    currentFunction.addInstr(new MipsInstruction(ADDU, ptrReg.getName(), ptrReg.getName(), temp.getName()));
                }

            }
        }
        saveInStack(getElementPtr, ptrReg);
    }

    public void genCompare(Compare compare) {
        for (Value user: compare.getUsersList()) {
            if (user instanceof Zext) { // 说明参与运算，需要分配寄存器
                Value value1 = compare.getOperands().get(0);
                Value value2 = compare.getOperands().get(1);
                MipsRegister temp = getReg(compare);
                MipsRegister op1;
                MipsRegister op2;
                if (value1 instanceof Constant) {
                    op1 = getReg(value1);
                    if (!value1.getName().equals("0")) {
                        currentFunction.addInstr(new MipsInstruction(LI, op1.getName(), value1.getName()));
                    }
                } else {
                    op1 = getReg(value1);
                }
                if (value2 instanceof Constant) {
                    op2 = getReg(value2);
                    if (!value2.getName().equals("0")) {
                        currentFunction.addInstr(new MipsInstruction(LI, op2.getName(), value2.getName()));
                    }
                } else {
                    op2 = getReg(value2);
                }
                genIcmpInstr(compare, temp, op1, op2);
                break;
            }
        }
    }

    public void genIcmpInstr(Compare instr, MipsRegister temp, MipsRegister op1, MipsRegister op2) {
        MipsInstruction compare;
        Compare.CondType type = instr.getCondType();
        switch (type) {
            case NE:
                compare = new MipsInstruction(SNE, temp.getName(), op1.getName(), op2.getName());
                break;
            case EQ:
                compare = new MipsInstruction(SEQ, temp.getName(), op1.getName(), op2.getName());
                break;
            case SGE:   // >= 就是 < 再取反
                currentFunction.addInstr(new MipsInstruction(SLT, "$v1", op1.getName(), op2.getName()));
                compare = new MipsInstruction(SEQ, temp.getName(), "$v1", "$zero");
                break;
            case SLE:   // <= 就是 > 再取反
                currentFunction.addInstr(new MipsInstruction(SLT, "$v1", op2.getName(), op1.getName()));
                compare = new MipsInstruction(SEQ, temp.getName(), "$v1", "$zero");
                break;
            case SGT:   // > 就是交换操作数的 <
                compare = new MipsInstruction(SLT, temp.getName(), op2.getName(), op1.getName());
                break;
            case SLT:
                compare = new MipsInstruction(SLT, temp.getName(), op1.getName(), op2.getName());
                break;
            default:
                compare = new MipsInstruction(NOP);
        }
        currentFunction.addInstr(compare);
        saveInStack(instr, temp);
    }

    public void genBranch(Branch branch) {
        currentFunction.addInstr(new MipsInstruction("# " + branch));
        if (branch.getOperands().size() == 1) { // 直接跳转
            if (curBlock.getNeighbour() != null && curBlock.getNeighbour().equals(branch.getOperands().get(0))) {
                return;
            }
            currentFunction.addInstr(new MipsInstruction(J, ((BasicBlock) branch.getOperands().get(0)).getLabel()));
            // currentFunction.addInstr(new MipsInstruction(NOP));
            return;
        }
        Value cond = branch.getOperands().get(0);
        if (cond instanceof Constant) {
            if (cond.getName().equals("0")) {
                currentFunction.addInstr(new MipsInstruction(J, ((BasicBlock) branch.getOperands().get(2)).getLabel()));
            } else {
                currentFunction.addInstr(new MipsInstruction(J, ((BasicBlock) branch.getOperands().get(1)).getLabel()));
            }
            // currentFunction.addInstr(new MipsInstruction(NOP));
            return;
        }
        Compare judge = (Compare) branch.getOperands().get(0);
        BasicBlock trueBranch = (BasicBlock) branch.getOperands().get(1);
        BasicBlock falseBranch = (BasicBlock) branch.getOperands().get(2);
        if (curBlock.getNeighbour().getName().equals(falseBranch.getName())) {  // 直接后继是trueBranch
            genBranchInstr(judge, trueBranch);
        } else {
            Compare compare = reverseIcmp(judge);
            genBranchInstr(compare, falseBranch);
            reverseIcmp(compare);
        }
    }

    public void genBranchInstr(Compare judge, BasicBlock target) {
        Value value1 = judge.getOperands().get(0);
        Value value2 = judge.getOperands().get(1);
        MipsRegister op1 = getReg(value1);
        MipsRegister op2 = getReg(value2);
        if (value1 instanceof Constant && !value1.getName().equals("0")) {
            currentFunction.addInstr(new MipsInstruction(LI, op1.getName(), value1.getName()));
        }
        if (value2 instanceof Constant && !value2.getName().equals("0")) {
            currentFunction.addInstr(new MipsInstruction(LI, op2.getName(), value2.getName()));
        }
        MipsInstruction branch;
        switch (judge.getCondType()) {
            case NE:
                branch = new MipsInstruction(BNE, op1.getName(), op2.getName(), target.getLabel());
                break;
            case EQ:
                branch = new MipsInstruction(BEQ, op1.getName(), op2.getName(), target.getLabel());
                break;
            case SGE:
                branch = new MipsInstruction(BGE, op1.getName(), op2.getName(), target.getLabel());
                break;
            case SLE:
                branch = new MipsInstruction(BLE, op1.getName(), op2.getName(), target.getLabel());
                break;
            case SGT:
                branch = new MipsInstruction(BGT, op1.getName(), op2.getName(), target.getLabel());
                break;
            case SLT:
                branch = new MipsInstruction(BLT, op1.getName(), op2.getName(), target.getLabel());
                break;
            default:
                branch = new MipsInstruction(NOP);
        }
        currentFunction.addInstr(branch);
        // currentFunction.addInstr(new MipsInstruction(NOP));
    }

    public Compare reverseIcmp(Compare compare) {
        switch (compare.getCondType()) {
            case NE:
                compare.setCondType(Compare.CondType.EQ);
                break;
            case EQ:
                compare.setCondType(Compare.CondType.NE);
                break;
            case SGE:
                compare.setCondType(Compare.CondType.SLT);
                break;
            case SLE:
                compare.setCondType(Compare.CondType.SGT);
                break;
            case SGT:
                compare.setCondType(Compare.CondType.SLE);
                break;
            case SLT:
                compare.setCondType(Compare.CondType.SGE);
                break;
            default:
                break;
        }
        return compare;
    }

    private boolean flag = true;

    /**
     * value可能是变量，也有可能是常量
     * @param value llvm中的Value
     * @return 虚拟寄存器
     */
    private MipsRegister getReg(Value value) {
        HashMap<Value, Integer> value2reg = irFunction.getGlobalRegsMap();
        HashSet<Value> value2Stack = irFunction.getValueInStack();
        if (value2reg.containsKey(value)) {
            int reg = value2reg.get(value);
            return regManager.getReg(reg);
        } else if (value2Stack.contains(value)) {
            int ptr = stackManager.getVirtualPtr(value.getFullName());
            if (flag) {
                currentFunction.addInstr(new MipsInstruction(LW, "$t8", "$sp", String.valueOf(ptr)));
                flag = false;
                return regManager.getReg(16);
            } else {
                currentFunction.addInstr(new MipsInstruction(LW, "$t9", "$sp", String.valueOf(ptr)));
                flag = true;
                return regManager.getReg(17);
            }
        } else {    // 临时常量等
            if (value instanceof Constant && value.getName().equals("0")) {
                return regManager.getReg(0);
            }
            if (flag) {
                flag = false;
                return regManager.getReg(16);
            }
            flag = true;
            return regManager.getReg(17);
        }
    }

    public void saveInStack(Value value, MipsRegister temp) {
        if (!irFunction.getGlobalRegsMap().containsKey(value) || irFunction.getValueInStack().contains(value)) {
            int ptr = stackManager.getVirtualPtr(value.getFullName());
            currentFunction.addInstr(new MipsInstruction(SW, temp.getName(), "$sp", String.valueOf(ptr)));
        }
    }

    public String getDataType(ValueType.DataType dataType, boolean isString) {
        if (isString) {
            return ".asciiz";
        }
        if (dataType == ValueType.DataType.Integer8Ty) {
            return ".byte";
        }
        return ".word";
    }
}
