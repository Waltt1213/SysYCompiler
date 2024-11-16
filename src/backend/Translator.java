package backend;

import backend.mips.MipsDataSeg;
import backend.mips.MipsInstrType;
import backend.mips.MipsInstruction;
import backend.mips.MipsRegister;
import llvmir.Module;
import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.*;
import llvmir.values.instr.*;

import static backend.mips.MipsInstrType.*;

import java.util.*;

public class Translator {
    private final Module module;
    private ArrayList<MipsDataSeg> dataSegment;
    private ArrayList<MipsInstruction> textSegment;
    private final RegManager regManager = new RegManager();
    private final StackManager stackManager = new StackManager();
    private BasicBlock curBlock;
    private int maxParams = 0;

    public Translator(Module module) {
        this.module = module;
        dataSegment = new ArrayList<>();
        textSegment = new ArrayList<>();
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
            MipsDataSeg data = new MipsDataSeg(name, dataType, init);
            dataSegment.add(data);
            stackManager.addGlobalData(globalVar.getFullName());
        }
    }

    public void genTextSegment() {
        ArrayList<Function> functions = module.getFunctions();
        calMaxParam(functions);
        Collections.reverse(functions); // 翻转list,先解析main
        for (Function function: functions) {
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
        MipsInstruction label = new MipsInstruction(declare.getName());
        textSegment.add(label);
        if (declare.getName().equals("putint")) {
            textSegment.add(new MipsInstruction(LI, "$v0", "1"));
        }
        if (declare.getName().equals("putch")) {
            textSegment.add(new MipsInstruction(LI, "$v0", "11"));
        }
        if (declare.getName().equals("putstr")) {
            textSegment.add(new MipsInstruction(LI, "$v0", "4"));
        }
        if (declare.getName().equals("getint")) {
            textSegment.add(new MipsInstruction(LI, "$v0", "5"));
        }
        if (declare.getName().equals("getchar")) {
            textSegment.add(new MipsInstruction(LI, "$v0", "12"));
        }
        textSegment.add(new MipsInstruction(SYSCALL, ""));
        textSegment.add(new MipsInstruction(JR, "$ra"));
    }

    public void genFunction(Function function) {
        textSegment.add(new MipsInstruction(function.getName()));
        // 函数序言
        allocStackFrame(function);
        // TODO: 如果是非叶子函数，则需要将a0->a3压栈
        for (BasicBlock basicBlock: function.getBasicBlocks()) {
            curBlock = basicBlock;
            genBasicBlock(basicBlock);
        }
        // 函数尾声
        stackManager.clear();
        regManager.clear();
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
        int size = function.getFuncFParams().size() * 4;
        // 从栈顶到栈底记录映射，依次为 argument, save reg， local var, fp, ra
        // 记录参数映射
        allocArguments(function);
        // 记录save reg映射
        for (int i = 0; i < 10; i++) {
            stackManager.putVirtualReg("$t" + i, 4);
            size += 4;
        }
        // 保存local var: MIPS在保存局部变量时，遵循的顺序是：先定义的变量后入栈
        size += saveLocalVariables(function);
        // 保存fp, ra
        size += 8;
        // 更新sp, fp寄存器:每个函数栈内部的fp都指向了调用者函数的栈顶
        textSegment.add(new MipsInstruction(MOVE, "$fp", "$sp"));
        textSegment.add(new MipsInstruction(ADDIU, "$sp", "$sp", "-" + size));
        // 将fp存入栈空间
        int ptrFp = stackManager.putVirtualReg("$fp", 4);
        // 存入ra（暂时不管是否为叶子函数）
        int ptrRa = stackManager.putVirtualReg("$ra", 4);
        textSegment.add(new MipsInstruction(SW, "$ra", "$sp", String.valueOf(ptrRa)));
        textSegment.add(new MipsInstruction(SW, "$fp", "$sp", String.valueOf(ptrFp)));
        // 调用者的传参在栈中位置构建字典
        for (Argument funcFParam : function.getFuncFParams()) {
            stackManager.putVirtualReg(funcFParam.getFullName(), 4);
        }
    }

    public void allocArguments(Function function) {
        ArrayList<Argument> funcFParams = function.getFuncFParams();
        // 设置函数参数与寄存器映射 a0->a3
        for (int i = 0; i < funcFParams.size() && i < 4; i++) {
            regManager.setArgueRegUse(funcFParams.get(i).getFullName(), 4 + i);
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
            textSegment.add(new MipsInstruction(basicBlock.getLabel()));
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
            textSegment.add(new MipsInstruction("# " + instruction));
        } else if (instruction instanceof GetElementPtr) {
            genElementPtr((GetElementPtr) instruction);
        } else if (instruction instanceof Compare) {
            textSegment.add(new MipsInstruction("# " + instruction));
        } else if (instruction instanceof Branch) {
            genBranch((Branch) instruction);
        }
    }

    public void genCall(Call call) {
        textSegment.add(new MipsInstruction("# " + call.toString()));
        Function function = call.getCallFunc();
        ArrayList<Argument> arguments = function.getFuncFParams();
        if (function.getName().equals("putstr")) {
            GetElementPtr getElementPtr = (GetElementPtr) call.getFuncRParams().get(0);
            textSegment.add(new MipsInstruction(LA, "$a0", getElementPtr.getOperands().get(0).getName()));
            // 跳转到目标函数
            textSegment.add(new MipsInstruction(JAL, function.getName()));
            if (function.isNotVoid()) {
                MipsRegister ret = regManager.getTempReg(call.getFullName());
                textSegment.add(new MipsInstruction(MOVE, ret.getName(), "$v0"));
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
                    textSegment.add(new MipsInstruction(LI, "$a" + i, constVar));
                } else {
                    MipsRegister argue = regManager.getReg(value.getFullName());
                    textSegment.add(new MipsInstruction(ADDU, "$a" + i, "$zero", argue.getName()));
                    regManager.resetTempReg(argue);
                }
            } else {
                int ptr = stackManager.getVirtualPtr("$a" + i);
                if (value instanceof Constant) {
                    MipsRegister temp = regManager.setTempRegUse(value.getFullName());
                    textSegment.add(new MipsInstruction(LI, temp.getName(), value.getName()));
                    textSegment.add(new MipsInstruction(SW, temp.getName(), "$sp", String.valueOf(ptr)));
                    regManager.resetTempReg(temp);
                } else {
                    MipsRegister argue = regManager.getTempReg(value.getFullName());
                    textSegment.add(new MipsInstruction(SW, argue.getName(), "$sp", String.valueOf(ptr)));
                    regManager.resetTempReg(argue);
                }
            }
        }
        // 保存现场
        HashMap<Integer, String> unused = regManager.unusedMap();
        for (Map.Entry<Integer, String> entry: unused.entrySet()) {
            MipsRegister tempReg = regManager.getReg(entry.getKey());
            int ptr = stackManager.getVirtualPtr(tempReg.getName());
            textSegment.add(new MipsInstruction(SW, tempReg.getName(), "$sp", String.valueOf(ptr)));
        }
        // 跳转到目标函数
        textSegment.add(new MipsInstruction(JAL, function.getName()));
        textSegment.add(new MipsInstruction(NOP));
        if (function.isNotVoid()) {
            MipsRegister ret = regManager.getTempReg(call.getFullName());
            textSegment.add(new MipsInstruction(MOVE, ret.getName(), "$v0"));
        }
        // 恢复现场
        HashMap<Integer, String> restoreMap = regManager.getRestoreMap();
        for (Map.Entry<Integer, String> entry: restoreMap.entrySet()) {
            MipsRegister temp = regManager.getReg(entry.getKey());
            int ptr = stackManager.getVirtualPtr(temp.getName());
            textSegment.add(new MipsInstruction(LW,temp.getName(), "$sp", String.valueOf(ptr)));
            regManager.getTempUseMap().put(entry.getKey(), entry.getValue());
        }
    }

    public void genStore(Store store) {
        textSegment.add(new MipsInstruction("# " + store.toString()));
        Value value = store.getOperands().get(0);
        Value addr = store.getOperands().get(1);
        MipsRegister reg = regManager.getReg(value.getFullName());
        if (value instanceof Constant) {
            textSegment.add(new MipsInstruction(LI, reg.getName(), value.getName()));
        }
        int ptr = 0;
        if ((ptr = stackManager.getVirtualPtr(addr.getFullName())) >= 0) {
            textSegment.add(new MipsInstruction(SW, reg.getName(), "$sp", String.valueOf(ptr)));
        } else {
            MipsRegister temp0;
            if (stackManager.isGlobalData(addr.getFullName())) {    // 全局变量
                temp0 = regManager.getTempReg(addr.getFullName()); // 加载全局变量地址
                textSegment.add(new MipsInstruction(LA, temp0.getName(), addr.getName()));
                textSegment.add(new MipsInstruction(SW, reg.getName(), temp0.getName(), "0"));
            } else {
                temp0 = regManager.getReg(addr.getFullName());
                textSegment.add(new MipsInstruction(SW, reg.getName(), temp0.getName(), "0"));
            }
            if (addr.getUsersList().size() == 1) {
                regManager.resetTempReg(temp0);
            }
        }
        regManager.resetTempReg(reg);
    }

    public void genLoad(Load load) {
        textSegment.add(new MipsInstruction("# " + load.toString()));
        Value addr = load.getOperands().get(0);
        int ptr = stackManager.getVirtualPtr(addr.getFullName());
        if (ptr >= 0) { // 临时变量
            MipsRegister temp = regManager.getTempReg(load.getFullName()); // load到临时寄存器
            textSegment.add(new MipsInstruction(LW, temp.getName(), "$sp", String.valueOf(ptr)));
        } else {
            if (stackManager.isGlobalData(addr.getFullName())) {    // 全局变量
                MipsRegister temp0 = regManager.getTempReg(addr.getFullName()); // 加载全局变量地址
                textSegment.add(new MipsInstruction(LA, temp0.getName(), addr.getName()));
                MipsRegister temp = regManager.getTempReg(load.getFullName()); // load到临时寄存器
                textSegment.add(new MipsInstruction(LW, temp.getName(), temp0.getName(), "0"));
                regManager.resetTempReg(temp0);
            } else {
                MipsRegister temp0 = regManager.getReg(addr.getFullName());
                MipsRegister temp = regManager.getTempReg(load.getFullName()); // load到临时寄存器
                textSegment.add(new MipsInstruction(LW, temp.getName(), temp0.getName(), "0"));
                regManager.resetTempReg(temp0);
            }
        }
    }

    public void genElementPtr(GetElementPtr getElementPtr) {    // 计算地址
        textSegment.add(new MipsInstruction("# " + getElementPtr.toString()));
        Value firstAddr = getElementPtr.getOperands().get(0);   // 可能来自reg，也可能来自栈
        Value bis1 = getElementPtr.getOperands().get(1);
        int ptr = 0;
        int firstPtr = stackManager.getVirtualPtr(firstAddr.getFullName());
        MipsRegister first;
        // 获取首地址
        if (firstPtr >= 0) {    // 来自栈
            first = regManager.getReg(29);
            ptr += firstPtr;
        } else {
            if (stackManager.isGlobalData(firstAddr.getFullName())) {
                first = regManager.getTempReg(firstAddr.getFullName()); // 加载全局变量地址
                textSegment.add(new MipsInstruction(LA, first.getName(), firstAddr.getName())); // 首地址为全局变量地址
            } else {
                first = regManager.getReg(firstAddr.getFullName());
            }
        }
        if (bis1 instanceof Constant) {
            ptr += Integer.parseInt(bis1.getName()) * 4; // 指针初始化
        } else {
            MipsRegister temp = regManager.getReg(bis1.getFullName());
            MipsRegister temp1 = regManager.getTempReg("*4");
            textSegment.add(new MipsInstruction(SLL, temp1.getName(), temp.getName(), String.valueOf(2)));
            textSegment.add(new MipsInstruction(ADDU, first.getName(), first.getName(), temp1.getName()));
            regManager.resetTempReg(temp1);
        }
        if (getElementPtr.getOperands().size() > 2) {
            Value bis2 = getElementPtr.getOperands().get(2);
            if (bis2 instanceof Constant) {
                ptr += Integer.parseInt(bis2.getName()) * 4;
                MipsRegister ptrReg = regManager.getTempReg(getElementPtr.getFullName());
                textSegment.add(new MipsInstruction(ADDIU, ptrReg.getName(), first.getName(), String.valueOf(ptr)));
            } else {
                MipsRegister temp = regManager.getReg(bis2.getFullName());
                // 先乘4
                MipsRegister temp1 = regManager.getTempReg("*4");
                textSegment.add(new MipsInstruction(SLL, temp1.getName(), temp.getName(), String.valueOf(2)));
                MipsRegister bis = regManager.getTempReg(getElementPtr.getFullName());
                textSegment.add(new MipsInstruction(ADDIU, bis.getName(), temp1.getName(), String.valueOf(ptr)));
                regManager.resetTempReg(temp);
                regManager.resetTempReg(temp1);
                MipsRegister ptrReg = regManager.getTempReg(getElementPtr.getFullName());
                textSegment.add(new MipsInstruction(ADDU, ptrReg.getName(), first.getName(), bis.getName()));
            }
        } else {
            MipsRegister ptrReg = regManager.getTempReg(getElementPtr.getFullName());
            textSegment.add(new MipsInstruction(ADDIU, ptrReg.getName(), first.getName(), String.valueOf(ptr)));
        }
        regManager.resetTempReg(first);
    }

    public void genReturn(Return ret, boolean isMain) {
        textSegment.add(new MipsInstruction("# " + ret.toString()));
        // 设置返回值
        if (!ret.getOperands().isEmpty()) {
            Value value = ret.getOperands().get(0);
            if (value instanceof Constant) {
                textSegment.add(new MipsInstruction(ADDI, "$v0", "$zero", value.getName()));
            } else {
                MipsRegister reg = regManager.getReg(value.getFullName());
                textSegment.add(new MipsInstruction(ADDU, "$v0", "$zero", reg.getName()));
                regManager.resetTempReg(reg);
            }
        }
        // 恢复ra
        int raPtr = stackManager.getVirtualPtr("$ra");
        textSegment.add(new MipsInstruction(LW, "$ra", "$sp", String.valueOf(raPtr)));
        // 恢复栈帧
        int ptr = stackManager.getVirtualPtr("$fp");
        textSegment.add(new MipsInstruction(LW, "$fp", "$sp", String.valueOf(ptr)));
        textSegment.add(new MipsInstruction(MOVE, "$sp", "$fp"));
        // 返回调用者
        if (isMain) {
            textSegment.add(new MipsInstruction(LI, "$v0", String.valueOf(10)));
            textSegment.add(new MipsInstruction(SYSCALL));
        } else {
            textSegment.add(new MipsInstruction(JR, "$ra"));
            textSegment.add(new MipsInstruction(NOP));
        }
    }

    public void genBinaryOperator(BinaryOperator binaryOperator) {
        textSegment.add(new MipsInstruction("# " + binaryOperator.toString()));
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
        if (operand1 instanceof Constant) {
            MipsRegister temp = regManager.getTempReg(binaryOperator.getFullName());
            MipsRegister reg = regManager.getReg(operand2.getFullName());
            textSegment.add(new MipsInstruction(ADDI, temp.getName(), reg.getName(), operand1.getName()));
            regManager.resetTempReg(reg);
        } else if (operand2 instanceof Constant) {
            MipsRegister temp = regManager.getTempReg(binaryOperator.getFullName());
            MipsRegister reg = regManager.getReg(operand1.getFullName());
            textSegment.add(new MipsInstruction(ADDI, temp.getName(), reg.getName(), operand2.getName()));
            regManager.resetTempReg(reg);
        } else {
            MipsRegister temp = regManager.getTempReg(binaryOperator.getFullName());
            MipsRegister op1 = regManager.getReg(operand1.getFullName());
            MipsRegister op2 = regManager.getReg(operand2.getFullName());
            textSegment.add(new MipsInstruction(ADDU, temp.getName(), op1.getName(), op2.getName()));
            regManager.resetTempReg(op1);
            regManager.resetTempReg(op2);
        }
    }

    public void genSubInstr(BinaryOperator binaryOperator) {
        Value operand1 = binaryOperator.getOperands().get(0);
        Value operand2 = binaryOperator.getOperands().get(1);
        if (operand1 instanceof Constant) {
            // 第一个操作数是常数，需要addi $t0, $zero, op1, 再subu $t2, $t0, $t1
            MipsRegister temp0 = regManager.getTempReg(operand1.getFullName());
            textSegment.add(new MipsInstruction(ADDI, temp0.getName(), "$zero", operand1.getName()));
            MipsRegister temp = regManager.getTempReg(binaryOperator.getFullName());
            MipsRegister reg = regManager.getReg(operand2.getFullName());
            textSegment.add(new MipsInstruction(SUBU, temp.getName(), temp0.getName(), reg.getName()));
            regManager.resetTempReg(reg);
            regManager.resetTempReg(temp0);
        } else if (operand2 instanceof Constant) {
            MipsRegister temp = regManager.getTempReg(binaryOperator.getFullName());
            MipsRegister reg = regManager.getReg(operand1.getFullName());
            textSegment.add(new MipsInstruction(SUBI, temp.getName(), reg.getName(), operand2.getName()));
            regManager.resetTempReg(reg);
        } else {
            MipsRegister temp = regManager.getTempReg(binaryOperator.getFullName());
            MipsRegister op1 = regManager.getReg(operand1.getFullName());
            MipsRegister op2 = regManager.getReg(operand2.getFullName());
            textSegment.add(new MipsInstruction(SUB, temp.getName(), op1.getName(), op2.getName()));
            regManager.resetTempReg(op1);
            regManager.resetTempReg(op2);
        }
    }

    public void genHiLoInst(BinaryOperator binaryOperator, Instruction.Type op) {
        Value operand1 = binaryOperator.getOperands().get(0);
        Value operand2 = binaryOperator.getOperands().get(1);
        MipsRegister op1 = null;
        MipsRegister op2 = null;
        if (operand1 instanceof Constant) {
            op1 = regManager.getTempReg(operand1.getName());
            textSegment.add(new MipsInstruction(ADDI, op1.getName(), "$zero", operand1.getName()));
        }
        if (operand2 instanceof Constant) {
            op2 = regManager.getTempReg(operand2.getName());
            textSegment.add(new MipsInstruction(ADDI, op2.getName(), "$zero", operand2.getName()));
        }
        if (op1 == null) {
            op1 = regManager.getReg(operand1.getFullName());
        }
        if (op2 == null) {
            op2 = regManager.getReg(operand2.getFullName());
        }
        MipsInstrType type = op == Instruction.Type.MUL ? MULT : DIV;
        textSegment.add(new MipsInstruction(type, op1.getName(), op2.getName()));
        regManager.resetTempReg(op1);
        regManager.resetTempReg(op2);
        MipsRegister temp = regManager.getTempReg(binaryOperator.getFullName());
        if (op == Instruction.Type.MUL || op == Instruction.Type.SDIV) {
            textSegment.add(new MipsInstruction(MFLO, temp.getName()));
        } else {
            textSegment.add(new MipsInstruction(MFHI, temp.getName()));
        }
    }

    public void genTrunc(Trunc trunc) {
        textSegment.add(new MipsInstruction("# " + trunc));
        Value op1 = trunc.getOperands().get(0);
        MipsRegister reg = regManager.getReg(op1.getFullName());
        MipsRegister temp = regManager.getTempReg(trunc.getFullName());
        textSegment.add(new MipsInstruction(ANDI, temp.getName(), reg.getName(), "0xFF"));
        regManager.resetTempReg(reg);
    }

    public void genZext(Zext zext) {
        textSegment.add(new MipsInstruction("# " + zext));
        Value op1 = zext.getOperands().get(0);
        MipsRegister reg = regManager.getReg(op1.getFullName());
        MipsRegister temp = regManager.getTempReg(zext.getFullName());
        textSegment.add(new MipsInstruction(ANDI, temp.getName(), reg.getName(), "0xFF"));
        regManager.resetTempReg(reg);
    }

    public void genBranch(Branch branch) {
        textSegment.add(new MipsInstruction("# " + branch));
        if (branch.getOperands().size() == 1) { // 直接跳转
            textSegment.add(new MipsInstruction(J, ((BasicBlock) branch.getOperands().get(0)).getLabel()));
            textSegment.add(new MipsInstruction(NOP));
            return;
        }
        Value cond = branch.getOperands().get(0);
        if (cond instanceof Constant) {
            if (cond.getName().equals("0")) {
                textSegment.add(new MipsInstruction(J, ((BasicBlock) branch.getOperands().get(2)).getLabel()));
            } else {
                textSegment.add(new MipsInstruction(J, ((BasicBlock) branch.getOperands().get(1)).getLabel()));
            }
            textSegment.add(new MipsInstruction(NOP));
            return;
        }
        Compare judge = (Compare) branch.getOperands().get(0);
        BasicBlock trueBranch = (BasicBlock) branch.getOperands().get(1);
        BasicBlock falseBranch = (BasicBlock) branch.getOperands().get(2);
        if (curBlock.getDirect().getName().equals(falseBranch.getName())) {  // 直接后继是trueBranch
            genIcmpInstr(judge, trueBranch);
        } else {
            Compare compare = reverseIcmp(judge);
            genIcmpInstr(compare, falseBranch);
        }
    }

    public void genIcmpInstr(Compare judge, BasicBlock target) {
        Value value1 = judge.getOperands().get(0);
        Value value2 = judge.getOperands().get(1);
        MipsRegister op1 = regManager.getReg(value1.getFullName());
        MipsRegister op2 = regManager.getReg(value2.getFullName());
        if (value1 instanceof Constant) {
            textSegment.add(new MipsInstruction(LI, op1.getName(), value1.getName()));
        }
        if (value2 instanceof Constant) {
            textSegment.add(new MipsInstruction(LI, op2.getName(), value2.getName()));
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
        textSegment.add(branch);
        textSegment.add(new MipsInstruction(NOP));
        regManager.resetTempReg(op1);
        regManager.resetTempReg(op2);
    }

    public Compare reverseIcmp(Compare compare) {
        Compare reverse;
        switch (compare.getCondType()) {
            case NE:
                reverse = new Compare(compare.getName(), Compare.CondType.EQ);
                break;
            case EQ:
                reverse = new Compare(compare.getName(), Compare.CondType.NE);
                break;
            case SGE:
                reverse = new Compare(compare.getName(), Compare.CondType.SLT);
                break;
            case SLE:
                reverse = new Compare(compare.getName(), Compare.CondType.SGT);
                break;
            case SGT:
                reverse = new Compare(compare.getName(), Compare.CondType.SLE);
                break;
            case SLT:
                reverse = new Compare(compare.getName(), Compare.CondType.SGE);
                break;
            default:
                reverse = compare;
        }
        if (!reverse.equals(compare)) {
            reverse.addOperands(compare.getOperands().get(0));
            reverse.addOperands(compare.getOperands().get(1));
        }
        return reverse;
    }

    public ArrayList<MipsDataSeg> getDataSegment() {
        return dataSegment;
    }

    public ArrayList<MipsInstruction> getTextSegment() {
        return textSegment;
    }

    public String getDataType(ValueType.DataType dataType, boolean isString) {
        if (isString) {
            return ".asciiz";
        }
        return ".word";
    }
}
