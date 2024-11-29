package backend.afteropt;

import backend.mips.*;
import backend.beforeopt.StackManager;
import llvmir.Module;
import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.*;
import llvmir.values.instr.*;

import java.util.*;

import static backend.mips.MipsInstrType.*;
import static backend.mips.MipsInstrType.SW;

/**
 * 后端分为两步：第一步直接将llvm转化成mips,仍使用虚拟寄存器；
 * 第二步将虚拟寄存器映射到物理寄存器上
 * Translator类用来实现第一步，核心点在于遵循mips规范的基础上使用无限个虚拟寄存器
 */
public class Translator {
    private final Module module;
    private MipsModule mipsModule;
    private StackManager stackManager = StackManager.getInstance();
    private HashMap<Value, VirtualRegister> value2virtualMap = new HashMap<>();
    private MipsFunction currentFunction;
    private BasicBlock curBlock;
    private int maxParams = 0;

    public Translator(Module module) {
        this.module = module;
        mipsModule = new MipsModule();
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
        // currentFunction.addInstr(new MipsInstruction(function.getName()));
        // 函数序言
        allocStackFrame(function);
        // TODO: 如果是非叶子函数，则需要将a0->a3压栈
        for (BasicBlock basicBlock: function.getBasicBlocks()) {
            curBlock = basicBlock;
            genBasicBlock(basicBlock);
        }
        // 函数尾声
        stackManager.clear();
        // regManager.clear();
        value2virtualMap.clear();
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
        // TODO： 当前方法只压入了 ra 和 fp
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
                stackManager.putVirtualReg(function.getFuncFParams().get(i).getFullName(), 4);
            } else {
                stackManager.addPtr(4);
            }
        }
    }

    public void allocArguments(Function function) {
        ArrayList<Argument> funcFParams = function.getFuncFParams();
        // 设置函数参数与寄存器映射 a0->a3
        for (int i = 0; i < funcFParams.size() && i < 4; i++) {
            // regManager.setArgueRegUse(funcFParams.get(i).getFullName(), 4 + i);
            VirtualRegister argue = new VirtualRegister(funcFParams.get(i).getFullName());
            value2virtualMap.put(funcFParams.get(i), argue);
        }
        // 记录argument->ptr映射
        for (int i = 0; i < maxParams; i++) {
            stackManager.putVirtualReg("$a" + i, 4); // TODO
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
            // genBranch((Branch) instruction);
        }
    }

    public void genStore(Store store) {
        currentFunction.addInstr(new MipsInstruction("# " + store.toString()));
        Value value = store.getOperands().get(0);
        Value addr = store.getOperands().get(1);
        // MipsRegister reg = regManager.getReg(value.getFullName());
        VirtualRegister reg = getVirtualReg(value);
        if (value instanceof Constant) {
            currentFunction.addInstr(new MipsInstruction(LI, reg.getName(), value.getName()));
        }
        int ptr = 0;
        if ((ptr = stackManager.getVirtualPtr(addr.getFullName())) >= 0) {  // 局部变量
            if (value instanceof Argument && Integer.parseInt(value.getName()) > 3) {
                int arguePtr = stackManager.getVirtualPtr(value.getFullName());
                currentFunction.addInstr(new MipsInstruction(LW, reg.getName(), "$sp", String.valueOf(arguePtr)));
            }
            currentFunction.addInstr(new MipsInstruction(SW, reg.getName(), "$sp", String.valueOf(ptr)));
        } else {
            VirtualRegister temp0;
            if (stackManager.isGlobalData(addr.getFullName())) {    // 全局变量
                temp0 = getVirtualReg(addr); // 加载全局变量地址
                currentFunction.addInstr(new MipsInstruction(LA, temp0.getName(), addr.getName()));
                currentFunction.addInstr(new MipsInstruction(SW, reg.getName(), temp0.getName(), "0"));
            } else {
                temp0 = getVirtualReg(addr);
                currentFunction.addInstr(new MipsInstruction(SW, reg.getName(), temp0.getName(), "0"));
            }
        }
    }

    public void genLoad(Load load) {
        currentFunction.addInstr(new MipsInstruction("# " + load.toString()));
        Value addr = load.getOperands().get(0);
        int ptr = stackManager.getVirtualPtr(addr.getFullName());
        if (ptr >= 0) { // 临时变量
            VirtualRegister temp = getVirtualReg(load); // load到临时寄存器
            currentFunction.addInstr(new MipsInstruction(LW, temp.getName(), "$sp", String.valueOf(ptr)));
        } else {
            if (stackManager.isGlobalData(addr.getFullName())) {    // 全局变量
                VirtualRegister temp0 = getVirtualReg(addr); // 加载全局变量地址
                currentFunction.addInstr(new MipsInstruction(LA, temp0.getName(), addr.getName()));
                VirtualRegister temp = getVirtualReg(load); // load到临时寄存器
                currentFunction.addInstr(new MipsInstruction(LW, temp.getName(), temp0.getName(), "0"));
                // regManager.resetTempReg(temp0);
            } else {
                VirtualRegister temp0 = getVirtualReg(addr);
                VirtualRegister temp = getVirtualReg(load); // load到临时寄存器
                currentFunction.addInstr(new MipsInstruction(LW, temp.getName(), temp0.getName(), "0"));
                // regManager.resetTempReg(temp0);
            }
        }
    }

    public void genReturn(Return ret, boolean isMain) {
        currentFunction.addInstr(new MipsInstruction("# " + ret.toString()));
        // 设置返回值
        if (!ret.getOperands().isEmpty()) {
            Value value = ret.getOperands().get(0);
            if (value instanceof Constant) {
                currentFunction.addInstr(new MipsInstruction(ADDI, "$v0", "$zero", value.getName()));
            } else {
                VirtualRegister reg = getVirtualReg(value);
                currentFunction.addInstr(new MipsInstruction(ADDU, "$v0", "$zero", reg.getName()));
                //regManager.resetTempReg(reg);
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
            currentFunction.addInstr(new MipsInstruction(NOP));
        }
    }

    public void genCall(Call call) {
        currentFunction.addInstr(new MipsInstruction("# " + call.toString()));
        Function function = call.getCallFunc();
        ArrayList<Argument> arguments = function.getFuncFParams();
        if (function.getName().equals("putstr")) {
            GetElementPtr getElementPtr = (GetElementPtr) call.getFuncRParams().get(0);
            currentFunction.addInstr(new MipsInstruction(LA, "$a0", getElementPtr.getOperands().get(0).getName()));
            // 跳转到目标函数
            currentFunction.addInstr(new MipsInstruction(JAL, function.getName()));
            if (function.isNotVoid()) {
                VirtualRegister ret = getVirtualReg(call);
                currentFunction.addInstr(new MipsInstruction(MOVE, ret.getName(), "$v0"));
            }
            return;
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
                    VirtualRegister argue = getVirtualReg(value);
                    currentFunction.addInstr(new MipsInstruction(ADDU, "$a" + i, "$zero", argue.getName()));
                }
            } else {
                int ptr = stackManager.getVirtualPtr("$a" + i);
                if (value instanceof Constant) {
                    VirtualRegister temp = getVirtualReg(value);
                    currentFunction.addInstr(new MipsInstruction(LI, temp.getName(), value.getName()));
                    currentFunction.addInstr(new MipsInstruction(SW, temp.getName(), "$sp", String.valueOf(ptr)));
                } else {
                    VirtualRegister argue = getVirtualReg(value);
                    currentFunction.addInstr(new MipsInstruction(SW, argue.getName(), "$sp", String.valueOf(ptr)));
                }
            }
        }
        // 保存现场
//        HashMap<Integer, String> unused = regManager.unusedMap();
//        for (Map.Entry<Integer, String> entry: unused.entrySet()) {
//            MipsRegister tempReg = regManager.getReg(entry.getKey());
//            int ptr = stackManager.getVirtualPtr(tempReg.getName());
//            currentFunction.addInstr(new MipsInstruction(SW, tempReg.getName(), "$sp", String.valueOf(ptr)));
//        }
        // 跳转到目标函数
        currentFunction.addInstr(new MipsInstruction(JAL, function.getName()));
        currentFunction.addInstr(new MipsInstruction(NOP));
        // 回到调用者
        if (function.isNotVoid()) {
            VirtualRegister ret = getVirtualReg(call);
            currentFunction.addInstr(new MipsInstruction(MOVE, ret.getName(), "$v0"));
        }
        // 恢复现场
//        HashMap<Integer, String> restoreMap = regManager.getRestoreMap();
//        for (Map.Entry<Integer, String> entry: restoreMap.entrySet()) {
//            MipsRegister temp = regManager.getReg(entry.getKey());
//            int ptr = stackManager.getVirtualPtr(temp.getName());
//            currentFunction.addInstr(new MipsInstruction(LW,temp.getName(), "$sp", String.valueOf(ptr)));
//        }
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
        VirtualRegister temp = getVirtualReg(binaryOperator);
        if (operand1 instanceof Constant) {
            VirtualRegister reg = getVirtualReg(operand2);
            currentFunction.addInstr(new MipsInstruction(ADDI, temp.getName(), reg.getName(), operand1.getName()));
            // regManager.resetTempReg(reg);
        } else if (operand2 instanceof Constant) {
            VirtualRegister reg = getVirtualReg(operand1);
            currentFunction.addInstr(new MipsInstruction(ADDI, temp.getName(), reg.getName(), operand2.getName()));
            // regManager.resetTempReg(reg);
        } else {
            VirtualRegister op1 = getVirtualReg(operand1);
            VirtualRegister op2 = getVirtualReg(operand2);
            currentFunction.addInstr(new MipsInstruction(ADDU, temp.getName(), op1.getName(), op2.getName()));
        }
    }

    public void genSubInstr(BinaryOperator binaryOperator) {
        Value operand1 = binaryOperator.getOperands().get(0);
        Value operand2 = binaryOperator.getOperands().get(1);
        VirtualRegister temp = getVirtualReg(binaryOperator);
        if (operand1 instanceof Constant) {
            // 第一个操作数是常数，需要addi $t0, $zero, op1, 再subu $t2, $t0, $t1
            VirtualRegister temp0 = getVirtualReg(operand1);
            currentFunction.addInstr(new MipsInstruction(ADDI, temp0.getName(), "$zero", operand1.getName()));
            VirtualRegister reg = getVirtualReg(operand2);
            currentFunction.addInstr(new MipsInstruction(SUBU, temp.getName(), temp0.getName(), reg.getName()));
        } else if (operand2 instanceof Constant) {
            VirtualRegister reg = getVirtualReg(operand1);
            currentFunction.addInstr(new MipsInstruction(SUBI, temp.getName(), reg.getName(), operand2.getName()));
        } else {
            VirtualRegister op1 = getVirtualReg(operand1);
            VirtualRegister op2 = getVirtualReg(operand2);
            currentFunction.addInstr(new MipsInstruction(SUBU, temp.getName(), op1.getName(), op2.getName()));
        }
    }

    public void genHiLoInst(BinaryOperator binaryOperator, Instruction.Type op) {
        Value operand1 = binaryOperator.getOperands().get(0);
        Value operand2 = binaryOperator.getOperands().get(1);
        VirtualRegister op1 = getVirtualReg(operand1);
        VirtualRegister op2 = getVirtualReg(operand2);
        if (operand1 instanceof Constant) {
            currentFunction.addInstr(new MipsInstruction(ADDI, op1.getName(), "$zero", operand1.getName()));
        }
        if (operand2 instanceof Constant) {
            currentFunction.addInstr(new MipsInstruction(ADDI, op2.getName(), "$zero", operand2.getName()));
        }
        MipsInstrType type = op == Instruction.Type.MUL ? MULT : DIV;
        currentFunction.addInstr(new MipsInstruction(type, op1.getName(), op2.getName()));
        VirtualRegister temp = getVirtualReg(binaryOperator);
        if (op == Instruction.Type.MUL || op == Instruction.Type.SDIV) {
            currentFunction.addInstr(new MipsInstruction(MFLO, temp.getName()));
        } else {
            currentFunction.addInstr(new MipsInstruction(MFHI, temp.getName()));
        }
    }

    public void genTrunc(Trunc trunc) {
        currentFunction.addInstr(new MipsInstruction("# " + trunc));
        Value op1 = trunc.getOperands().get(0);
        VirtualRegister reg = getVirtualReg(op1);
        value2virtualMap.put(trunc, reg);
    }

    public void genZext(Zext zext) {
        currentFunction.addInstr(new MipsInstruction("# " + zext));
        Value op1 = zext.getOperands().get(0);
        VirtualRegister reg = getVirtualReg(op1);
        value2virtualMap.put(zext, reg);
    }

    public void genElementPtr(GetElementPtr getElementPtr) {    // 计算地址
        currentFunction.addInstr(new MipsInstruction("# " + getElementPtr.toString()));
        Value firstAddr = getElementPtr.getOperands().get(0);   // 可能来自reg，也可能来自栈
        Value bis1 = getElementPtr.getOperands().get(1);
        int ptr = 0;
        int firstPtr = stackManager.getVirtualPtr(firstAddr.getFullName());
        VirtualRegister first;
        VirtualRegister ptrReg;
        // 获取首地址
        if (firstPtr >= 0) {    // 来自栈
            first = VirtualRegister.vSp;
            ptr += firstPtr;
        } else {
            first = getVirtualReg(firstAddr);
            if (stackManager.isGlobalData(firstAddr.getFullName())) {
                 // 加载全局变量地址
                currentFunction.addInstr(new MipsInstruction(LA, first.getName(), firstAddr.getName())); // 首地址为全局变量地址
            }
        }
        if (bis1 instanceof Constant) {
            ptr += Integer.parseInt(bis1.getName()) * 4; // 指针初始化
        } else {
            VirtualRegister temp = getVirtualReg(bis1);
            currentFunction.addInstr(new MipsInstruction(SLL, temp.getName(), temp.getName(), String.valueOf(2)));
            currentFunction.addInstr(new MipsInstruction(ADDU, first.getName(), first.getName(), temp.getName()));
        }
        if (getElementPtr.getOperands().size() > 2) {
            Value bis2 = getElementPtr.getOperands().get(2);
            if (bis2 instanceof Constant) {
                ptr += Integer.parseInt(bis2.getName()) * 4;
                ptrReg = getVirtualReg(getElementPtr);
                currentFunction.addInstr(new MipsInstruction(ADDIU, ptrReg.getName(), first.getName(), String.valueOf(ptr)));
            } else {
                VirtualRegister temp = getVirtualReg(bis2);
                // 先乘4
                currentFunction.addInstr(new MipsInstruction(SLL, temp.getName(), temp.getName(), String.valueOf(2)));
                ptrReg = getVirtualReg(getElementPtr);
                currentFunction.addInstr(new MipsInstruction(ADDIU, ptrReg.getName(), temp.getName(), String.valueOf(ptr)));
                currentFunction.addInstr(new MipsInstruction(ADDU, ptrReg.getName(), first.getName(), ptrReg.getName()));
            }
        } else {
            ptrReg = getVirtualReg(getElementPtr);
            currentFunction.addInstr(new MipsInstruction(ADDIU, ptrReg.getName(), first.getName(), String.valueOf(ptr)));
        }
    }

    public void genCompare(Compare compare) {
        for (Value user: compare.getUsersList()) {
            if (user instanceof Zext) { // 说明参与运算，需要分配寄存器
                Value value1 = compare.getOperands().get(0);
                Value value2 = compare.getOperands().get(1);
                VirtualRegister temp = getVirtualReg(user);
                VirtualRegister op1;
                VirtualRegister op2;
                if (value1 instanceof Constant) {
                    if (value1.getName().equals("0")) {
                        op1 = VirtualRegister.vZero;
                    } else {
                        op1 = getVirtualReg(value1);
                        currentFunction.addInstr(new MipsInstruction(LI, op1.getName(), value1.getName()));
                    }
                } else {
                    op1 = getVirtualReg(value1);
                }
                if (value2 instanceof Constant) {
                    if (value2.getName().equals("0")) {
                        op2 = VirtualRegister.vZero;
                    } else {
                        op2 = getVirtualReg(value2);
                        currentFunction.addInstr(new MipsInstruction(LI, op2.getName(), value2.getName()));
                    }
                } else {
                    op2 = getVirtualReg(value2);
                }
                genIcmpInstr(compare.getCondType(), temp, op1, op2);
            }
        }
    }

    public void genIcmpInstr(Compare.CondType type, VirtualRegister temp, VirtualRegister op1, VirtualRegister op2) {
        MipsInstruction compare;
        switch (type) {
            case NE:
                compare = new MipsInstruction(SNE, temp.getName(), op1.getName(), op2.getName());
                break;
            case EQ:
                compare = new MipsInstruction(SEQ, temp.getName(), op1.getName(), op2.getName());
                break;
            case SGE:   // >= 就是 < 再取反
                currentFunction.addInstr(new MipsInstruction(SLT, temp.getName(), op1.getName(), op2.getName()));
                compare = new MipsInstruction(SEQ, temp.getName(), temp.getName(), "$zero");
                break;
            case SLE:   // <= 就是 > 再取反
                currentFunction.addInstr(new MipsInstruction(SLT, temp.getName(), op2.getName(), op1.getName()));
                compare = new MipsInstruction(SEQ, temp.getName(), temp.getName(), "$zero");
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
    }

    /**
     * value可能是变量，也有可能是常量
     * @param value llvm中的Value
     * @return 虚拟寄存器
     */
    private VirtualRegister getVirtualReg(Value value) {
        if (value2virtualMap.containsKey(value)) {
            return value2virtualMap.get(value);
        }
        VirtualRegister reg = new VirtualRegister(value.getId() + value.getName());
        value2virtualMap.put(value, reg);
        return reg;
    }

    public String getDataType(ValueType.DataType dataType, boolean isString) {
        if (isString) {
            return ".asciiz";
        }
        return ".word";
    }
}
