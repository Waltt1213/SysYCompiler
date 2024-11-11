package backend;

import backend.mips.MipsDataSeg;
import backend.mips.MipsInstruction;
import backend.mips.MipsRegister;
import llvmir.Module;
import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.Argument;
import llvmir.values.BasicBlock;
import llvmir.values.Function;
import llvmir.values.GlobalVariable;
import llvmir.values.instr.Alloca;
import llvmir.values.instr.Instruction;
import llvmir.values.instr.Store;

import static backend.mips.MipsInstrType.*;

import java.util.*;

public class Translator {
    private final Module module;
    private ArrayList<MipsDataSeg> dataSegment;
    private ArrayList<MipsInstruction> textSegment;
    private final RegManager regManager = new RegManager();
    private final StackManager stackManager = new StackManager();

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
        }
    }

    public void genTextSegment() {
        ArrayList<Function> functions = module.getFunctions();
        Collections.reverse(functions); // 翻转list,先解析main
        for (Function function: functions) {
            genFunction(function);
        }
        HashMap<String, Function> declares = module.getDeclares();
        for (Function declare: declares.values()) {
            genDeclare(declare);
        }
    }

    public void genDeclare(Function declare) {
        // 添加函数标签
        MipsInstruction label = new MipsInstruction(declare.getName());
        textSegment.add(label);
        if (declare.getName().equals("putch") || declare.getName().equals("putint")) {
            textSegment.add(new MipsInstruction(LI, "$v0", "1"));
        }
        if (declare.getName().equals("putstr")) {
            textSegment.add(new MipsInstruction(LI, "$v0", "4"));
        }
        if (declare.getName().equals("getint") || declare.getName().equals("getchar")) {
            textSegment.add(new MipsInstruction(LI, "$v0", "5"));
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
        for (int i = 18; i < 26; i++) {
            stackManager.putVirtualReg("$s" + (i - 18), 4);
            size += 4;
        }
        // 保存local var: MIPS在保存局部变量时，遵循的顺序是：先定义的变量后入栈
        size += saveLocalVariables(function);
        // 保存fp, ra
        size += 8;
        // 更新sp, fp寄存器:每个函数栈内部的fp都指向了调用者函数的栈顶
        textSegment.add(new MipsInstruction(MOVE, "$fp", "$sp"));
        textSegment.add(new MipsInstruction(SUBU, "$sp", "$sp", String.valueOf(size)));
        // 将fp存入栈空间
        int ptrFp = stackManager.putVirtualReg("$fp", 4);
        // 存入ra（暂时不管是否为叶子函数）
        int ptrRa = stackManager.putVirtualReg("$ra", 4);
        textSegment.add(new MipsInstruction(SW, "$ra", "$sp", String.valueOf(ptrRa)));
        textSegment.add(new MipsInstruction(SW, "$fp", "$sp", String.valueOf(ptrFp)));
    }

    public void allocArguments(Function function) {
        ArrayList<Argument> funcFParams = function.getFuncFParams();
        // 设置函数参数与寄存器映射 a0->a3
        for (int i = 0; i < funcFParams.size() && i < 4; i++) {
            regManager.setArgueRegUse(funcFParams.get(i).getFullName(), 4 + i);
        }
        // 记录argument->ptr映射
        for (Argument funcFParam : funcFParams) {
            stackManager.putVirtualReg(funcFParam.getFullName(), 4);
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
        for (Instruction instruction: basicBlock.getInstructions()) {
            genInstruction(instruction);
        }
    }

    public void genInstruction(Instruction instruction) {
        if (instruction instanceof Store) {
            genStore((Store) instruction);
        }
    }

    public void genStore(Store store) {
        Value value = store.getOperands().get(0);
        Value addr = store.getOperands().get(1);
        int ptr = 0;
        if ((ptr = stackManager.getVirtualPtr(addr.getFullName())) > 0) {
            MipsRegister argue;
            if ((argue = regManager.getArgueReg(value.getFullName())) != null) {
                // 参数传递
                textSegment.add(new MipsInstruction(SW, argue.getName(), "$sp", String.valueOf(ptr)));
            }
        }
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
        if (dataType == ValueType.DataType.Integer32Ty) {
            return ".word";
        }
        return ".byte";
    }
}
