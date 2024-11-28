package backend.normal;

import backend.mips.MipsData;
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

public class OldTranslator {
    private final Module module;
    private ArrayList<MipsData> dataSegment;
    private ArrayList<MipsInstruction> textSegment;
    private final RegManager regManager = new RegManager();
    private final StackManager stackManager = StackManager.getInstance();
    private BasicBlock curBlock;
    private int maxParams = 0;

    public OldTranslator(Module module) {
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
            MipsData data = new MipsData(name, dataType, init);
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
        int size = maxParams * 4;
        // 从栈顶到栈底记录映射，依次为 save reg， local var, fp, ra, arguments
        // 记录参数映射
        allocArguments(function);
        // 记录save reg映射
        for (int i = 0; i < 10; i++) {
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
        textSegment.add(new MipsInstruction(MOVE, "$fp", "$sp"));
        textSegment.add(new MipsInstruction(ADDIU, "$sp", "$sp", "-" + size));
        // 将fp存入栈空间
        int ptrFp = stackManager.putVirtualReg("$fp", 4);
        // 存入ra（暂时不管是否为叶子函数）
        int ptrRa = stackManager.putVirtualReg("$ra", 4);
        textSegment.add(new MipsInstruction(SW, "$ra", "$sp", String.valueOf(ptrRa)));
        textSegment.add(new MipsInstruction(SW, "$fp", "$sp", String.valueOf(ptrFp)));
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
            regManager.setArgueRegUse(funcFParams.get(i).getFullName(), 4 + i);
        }
        // 记录被调用方参数->ptr映射
        for (int i = 0; i < maxParams; i++) {
            stackManager.setVirtualReg("$a" + i, -4 * (maxParams - i)); // TODO
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
            genCompare((Compare) instruction);
        } else if (instruction instanceof Branch) {
            genBranch((Branch) instruction);
        }
    }

    public MipsRegister getRegForVirtual(String value) {
        if (stackManager.inStack(value)) {
            MipsRegister temp = getReg(value);
            int ptr = stackManager.getVirtualPtr(value);
            textSegment.add(new MipsInstruction(LW, temp.getName(), "$sp", String.valueOf(ptr)));
            return temp;
        } else {
            return getReg(value);
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
                MipsRegister ret = getRegForVirtual(call.getFullName());
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
                    MipsRegister argue = getRegForVirtual(value.getFullName());
                    regManager.resetTempReg(argue);
                    textSegment.add(new MipsInstruction(ADDU, "$a" + i, "$zero", argue.getName()));
                }
            } else {
                int ptr = stackManager.getVirtualPtr("$a" + i);
                if (value instanceof Constant) {
                    MipsRegister temp = getReg(value.getFullName());
                    ptr = stackManager.getVirtualPtr("$a" + i);
                    textSegment.add(new MipsInstruction(LI, temp.getName(), value.getName()));
                    textSegment.add(new MipsInstruction(SW, temp.getName(), "$sp", String.valueOf(ptr)));
                    regManager.resetTempReg(temp);
                } else {
                    MipsRegister argue = getRegForVirtual(value.getFullName());
                    ptr = stackManager.getVirtualPtr("$a" + i);
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
        // 回到调用者
        if (function.isNotVoid()) {
            MipsRegister ret = getReg(call.getFullName());
            textSegment.add(new MipsInstruction(MOVE, ret.getName(), "$v0"));
        }
        // 恢复现场
        HashMap<Integer, String> restoreMap = regManager.getRestoreMap();
        for (Map.Entry<Integer, String> entry: restoreMap.entrySet()) {
            MipsRegister temp = regManager.getReg(entry.getKey());
            int ptr = stackManager.getVirtualPtr(temp.getName());
            textSegment.add(new MipsInstruction(LW,temp.getName(), "$sp", String.valueOf(ptr)));
        }
    }

    public void genStore(Store store) {
        textSegment.add(new MipsInstruction("# " + store.toString()));
        Value value = store.getOperands().get(0);
        Value addr = store.getOperands().get(1);
        MipsRegister reg = getReg(value.getFullName());
        if (value instanceof Constant) {
            textSegment.add(new MipsInstruction(LI, reg.getName(), value.getName()));
        }
        int ptr = 0;
        if ((ptr = stackManager.getVirtualPtr(addr.getFullName())) >= 0) {
            if (value instanceof Argument && Integer.parseInt(value.getName()) > 3) {
                int arguePtr = stackManager.getVirtualPtr(value.getFullName());
                textSegment.add(new MipsInstruction(LW, reg.getName(), "$sp", String.valueOf(arguePtr)));
            }
            textSegment.add(new MipsInstruction(SW, reg.getName(), "$sp", String.valueOf(ptr)));
        } else {
            MipsRegister temp0;
            if (stackManager.isGlobalData(addr.getFullName())) {    // 全局变量
                if (value.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                    textSegment.add(new MipsInstruction(SB, reg.getName(), addr.getName()));
                } else {
                    textSegment.add(new MipsInstruction(SW, reg.getName(), addr.getName()));
                }
            } else {
                temp0 = getReg(addr.getFullName());
                if (value.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                    textSegment.add(new MipsInstruction(SB, reg.getName(), temp0.getName(), "0"));
                } else {
                    textSegment.add(new MipsInstruction(SW, reg.getName(), temp0.getName(), "0"));
                }
                if (addr.getUsersList().size() == 1) {
                    regManager.resetTempReg(temp0);
                }
            }
        }
        regManager.resetTempReg(reg);
    }

    public void genLoad(Load load) {
        textSegment.add(new MipsInstruction("# " + load.toString()));
        Value addr = load.getOperands().get(0);
        int ptr = stackManager.getVirtualPtr(addr.getFullName());
        if (ptr >= 0) { // 临时变量
            MipsRegister temp = getReg(load.getFullName()); // load到临时寄存器
            ptr = stackManager.getVirtualPtr(addr.getFullName());
            textSegment.add(new MipsInstruction(LW, temp.getName(), "$sp", String.valueOf(ptr)));
        } else {
            if (stackManager.isGlobalData(addr.getFullName())) {    // 全局变量
                MipsRegister temp = getReg(load.getFullName()); // load到临时寄存器
                textSegment.add(new MipsInstruction(LA, temp.getName(), addr.getName()));
                if (load.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                    textSegment.add(new MipsInstruction(LB, temp.getName(), temp.getName(), "0"));
                } else {
                    textSegment.add(new MipsInstruction(LW, temp.getName(), temp.getName(), "0"));
                }
            } else {
                MipsRegister temp0 = getReg(addr.getFullName());
                regManager.resetTempReg(temp0);
                MipsRegister temp = getReg(load.getFullName()); // load到临时寄存器
                if (load.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                    textSegment.add(new MipsInstruction(LB, temp.getName(), temp0.getName(), "0"));
                } else {
                    textSegment.add(new MipsInstruction(LW, temp.getName(), temp0.getName(), "0"));
                }
            }
        }
    }

    public void genElementPtr(GetElementPtr getElementPtr) {    // 计算地址
        textSegment.add(new MipsInstruction("# " + getElementPtr.toString()));
        Value firstAddr = getElementPtr.getOperands().get(0);   // 可能来自reg，也可能来自栈
        Value bis1 = getElementPtr.getOperands().get(1);
        int firstPtr = stackManager.getVirtualPtr(firstAddr.getFullName());
        MipsRegister ptrReg = getReg(getElementPtr.getFullName());;
        // 获取首地址
        if (firstPtr >= 0) {
            textSegment.add(new MipsInstruction(ADDIU, ptrReg.getName(), "$sp", String.valueOf(firstPtr)));
        } else {
            if (stackManager.isGlobalData(firstAddr.getFullName())) {
                textSegment.add(new MipsInstruction(LA, ptrReg.getName(), firstAddr.getName()));
            } else {
                MipsRegister temp = getReg(firstAddr.getFullName());
                regManager.resetTempReg(temp);
                textSegment.add(new MipsInstruction(ADDU, ptrReg.getName(), "$zero", temp.getName()));
            }
        }
        // 加上第一部分索引
        if (bis1 instanceof Constant) {
            int ptr = Integer.parseInt(bis1.getName()) * 4;
            if (firstAddr.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                ptr = Integer.parseInt(bis1.getName());
            }
            textSegment.add(new MipsInstruction(ADDIU, ptrReg.getName(), ptrReg.getName(), String.valueOf(ptr)));
        } else {
            MipsRegister temp = getReg(bis1.getFullName());
            regManager.resetTempReg(temp);
            if (firstAddr.getTp().getDataType() == ValueType.DataType.Integer32Ty) {
                textSegment.add(new MipsInstruction(SLL, temp.getName(), temp.getName(), String.valueOf(2)));
            }
            // textSegment.add(new MipsInstruction(SLL, temp.getName(), temp.getName(), String.valueOf(2)));
            textSegment.add(new MipsInstruction(ADDU, ptrReg.getName(), ptrReg.getName(), temp.getName()));
        }
        if (getElementPtr.getOperands().size() > 2) {
            Value bis2 = getElementPtr.getOperands().get(2);
            if (bis2 instanceof Constant) {
                int ptr = Integer.parseInt(bis2.getName()) * 4;
                if (firstAddr.getTp().getDataType() == ValueType.DataType.Integer8Ty) {
                    ptr = Integer.parseInt(bis2.getName());
                }
                textSegment.add(new MipsInstruction(ADDIU, ptrReg.getName(), ptrReg.getName(), String.valueOf(ptr)));
            } else {
                MipsRegister temp = getReg(bis2.getFullName());
                regManager.resetTempReg(temp);
                if (firstAddr.getTp().getDataType() == ValueType.DataType.Integer32Ty) {
                    textSegment.add(new MipsInstruction(SLL, temp.getName(), temp.getName(), String.valueOf(2)));
                }
                // textSegment.add (new MipsInstruction(SLL, temp.getName(), temp.getName(), String.valueOf(2)));
                textSegment.add(new MipsInstruction(ADDU, ptrReg.getName(), ptrReg.getName(), temp.getName()));
            }
        }
    }

    public void genReturn(Return ret, boolean isMain) {
        textSegment.add(new MipsInstruction("# " + ret.toString()));
        // 设置返回值
        if (!ret.getOperands().isEmpty()) {
            Value value = ret.getOperands().get(0);
            if (value instanceof Constant) {
                textSegment.add(new MipsInstruction(ADDIU, "$v0", "$zero", value.getName()));
            } else {
                MipsRegister reg = getReg(value.getFullName());
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
        MipsRegister temp;
        if (operand1 instanceof Constant) {
            MipsRegister reg = getReg(operand2.getFullName());
            regManager.resetTempReg(reg);
            temp = getReg(binaryOperator.getFullName());
            textSegment.add(new MipsInstruction(ADDIU, temp.getName(), reg.getName(), operand1.getName()));
        } else if (operand2 instanceof Constant) {
            MipsRegister reg = getReg(operand1.getFullName());
            regManager.resetTempReg(reg);
            temp = getReg(binaryOperator.getFullName());
            textSegment.add(new MipsInstruction(ADDIU, temp.getName(), reg.getName(), operand2.getName()));
        } else {
            temp = getReg(binaryOperator.getFullName());
            MipsRegister op1 = getReg(operand1.getFullName());
            MipsRegister op2 = getReg(operand2.getFullName());
            textSegment.add(new MipsInstruction(ADDU, temp.getName(), op1.getName(), op2.getName()));
            regManager.resetTempReg(op1);
            regManager.resetTempReg(op2);
        }
    }

    public void genSubInstr(BinaryOperator binaryOperator) {
        Value operand1 = binaryOperator.getOperands().get(0);
        Value operand2 = binaryOperator.getOperands().get(1);
        MipsRegister temp;
        if (operand1 instanceof Constant) {
            // 第一个操作数是常数，需要addi $t0, $zero, op1, 再subu $t2, $t0, $t1
            MipsRegister temp0 = getReg(operand1.getFullName());
            textSegment.add(new MipsInstruction(ADDIU, temp0.getName(), "$zero", operand1.getName()));
            temp = getReg(binaryOperator.getFullName());
            MipsRegister reg = getReg(operand2.getFullName());
            textSegment.add(new MipsInstruction(SUBU, temp.getName(), temp0.getName(), reg.getName()));
            regManager.resetTempReg(reg);
            regManager.resetTempReg(temp0);
        } else if (operand2 instanceof Constant) {
            temp = getReg(binaryOperator.getFullName());
            MipsRegister reg = getReg(operand1.getFullName());
            textSegment.add(new MipsInstruction(SUBI, temp.getName(), reg.getName(), operand2.getName()));
            regManager.resetTempReg(reg);
        } else {
            temp = getReg(binaryOperator.getFullName());
            MipsRegister op1 = getReg(operand1.getFullName());
            MipsRegister op2 = getReg(operand2.getFullName());
            textSegment.add(new MipsInstruction(SUBU, temp.getName(), op1.getName(), op2.getName()));
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
            op1 = getReg(operand1.getName());
            textSegment.add(new MipsInstruction(ADDIU, op1.getName(), "$zero", operand1.getName()));
        }
        if (operand2 instanceof Constant) {
            op2 = getReg(operand2.getName());
            textSegment.add(new MipsInstruction(ADDIU, op2.getName(), "$zero", operand2.getName()));
        }
        if (op1 == null) {
            op1 = getReg(operand1.getFullName());
        }
        if (op2 == null) {
            op2 = getReg(operand2.getFullName());
        }
        MipsInstrType type = (op == Instruction.Type.MUL) ? MULT : DIV;
        textSegment.add(new MipsInstruction(type, op1.getName(), op2.getName()));
        regManager.resetTempReg(op1);
        regManager.resetTempReg(op2);
        MipsRegister temp = getReg(binaryOperator.getFullName());
        if (op == Instruction.Type.MUL || op == Instruction.Type.SDIV) {
            textSegment.add(new MipsInstruction(MFLO, temp.getName()));
        } else {
            textSegment.add(new MipsInstruction(MFHI, temp.getName()));
        }
    }

    public void genTrunc(Trunc trunc) {
        Value op1 = trunc.getOperands().get(0);
        MipsRegister reg = getReg(op1.getFullName());
        regManager.resetTempReg(reg);
        MipsRegister temp = getRegForVirtual(trunc.getFullName());
        textSegment.add(new MipsInstruction(ANDI, temp.getName(), reg.getName(), "0xFF"));

//        MipsRegister reg = getReg(op1.getFullName());
//        regManager.getTempUseMap().put(reg.getNo(), trunc.getFullName());
    }

    public void genZext(Zext zext) {
        Value op1 = zext.getOperands().get(0);
        MipsRegister reg = getReg(op1.getFullName());
        regManager.resetTempReg(reg);
        MipsRegister temp = getRegForVirtual(zext.getFullName());
        textSegment.add(new MipsInstruction(ANDI, temp.getName(), reg.getName(), "0xFF"));

//        MipsRegister reg = getReg(op1.getFullName());
//        regManager.getTempUseMap().put(reg.getNo(), zext.getFullName());
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
        if (curBlock.getNeighbour().getName().equals(falseBranch.getName())) {  // 直接后继是trueBranch
            genBranchInstr(judge, trueBranch);
        } else {
            Compare compare = reverseIcmp(judge);
            genBranchInstr(compare, falseBranch);
        }
    }

    public void genBranchInstr(Compare judge, BasicBlock target) {
        Value value1 = judge.getOperands().get(0);
        Value value2 = judge.getOperands().get(1);
        MipsRegister op1 = getReg(value1.getFullName());
        MipsRegister op2 = getReg(value2.getFullName());
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

    public void genCompare(Compare compare) {
        for (Value user: compare.getUsersList()) {
            if (user instanceof Zext) { // 说明参与运算，需要分配寄存器
                Value value1 = compare.getOperands().get(0);
                Value value2 = compare.getOperands().get(1);
                MipsRegister temp = getReg(compare.getFullName());
                MipsRegister op1;
                MipsRegister op2;
                if (value1 instanceof Constant) {
                    if (value1.getName().equals("0")) {
                        op1 = regManager.getReg(0);
                    } else {
                        op1 = getReg("compare1");
                        textSegment.add(new MipsInstruction(LI, op1.getName(), value1.getName()));
                    }
                } else {
                    op1 = getReg(value1.getFullName());
                }
                if (value2 instanceof Constant) {
                    if (value2.getName().equals("0")) {
                        op2 = regManager.getReg(0);
                    } else {
                        op2 = getReg("compare2");
                        textSegment.add(new MipsInstruction(LI, op2.getName(), value2.getName()));
                    }
                } else {
                    op2 = getReg(value2.getFullName());
                }
                genIcmpInstr(compare.getCondType(), temp, op1, op2);
            }
        }
    }

    public void genIcmpInstr(Compare.CondType type, MipsRegister temp, MipsRegister op1, MipsRegister op2) {
        MipsInstruction compare;
        switch (type) {
            case NE:
                compare = new MipsInstruction(SNE, temp.getName(), op1.getName(), op2.getName());
                break;
            case EQ:
                compare = new MipsInstruction(SEQ, temp.getName(), op1.getName(), op2.getName());
                break;
            case SGE:   // >= 就是 < 再取反
                textSegment.add(new MipsInstruction(SLT, temp.getName(), op1.getName(), op2.getName()));
                compare = new MipsInstruction(SEQ, temp.getName(), temp.getName(), "$zero");
                break;
            case SLE:   // <= 就是 > 再取反
                textSegment.add(new MipsInstruction(SLT, temp.getName(), op2.getName(), op1.getName()));
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
        textSegment.add(compare);
        regManager.resetTempReg(op1);
        regManager.resetTempReg(op2);
    }

    public MipsRegister getReg(String virtual) {
        if (virtual.equals("0")) {
            return regManager.getReg(0);
        }
        MipsRegister reg = regManager.getReg(virtual);
        // 没有说明没映射到寄存器上过
        if (reg == null) {
            reg = regManager.setTempRegUse(virtual);
        }
        // 寄存器池空了
        if (reg == null) {
            spill();
            return regManager.getDiscardReg();
        }
        return reg;
    }

    public void spill() {
        // 旧的值sw进栈
        textSegment.add(new MipsInstruction(ADDIU, "$sp", "$sp", String.valueOf(-4)));
        textSegment.add(new MipsInstruction(SW, regManager.getDiscardReg().getName(), "$sp", "0"));
        stackManager.setVirtualReg(regManager.getDiscardVirtual(), 0);
    }

    public ArrayList<MipsData> getDataSegment() {
        return dataSegment;
    }

    public ArrayList<MipsInstruction> getTextSegment() {
        return textSegment;
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
