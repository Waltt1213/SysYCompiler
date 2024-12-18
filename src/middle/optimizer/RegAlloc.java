package middle.optimizer;

import backend.mips.MipsRegister;
import llvmir.Module;
import llvmir.Value;
import llvmir.values.*;
import llvmir.values.instr.Alloca;
import llvmir.values.instr.Call;
import llvmir.values.instr.Instruction;
import llvmir.values.instr.Phi;

import java.util.*;

/**
 * 基于线性扫描法进行全局寄存器分配
 */
public class RegAlloc {
    private final Module module;
    private HashMap<Value, Integer> value2reg = new HashMap<>();
    private final HashMap<Integer, Value> reg2Value = new HashMap<>();
    private final ArrayList<Integer> freeRegsPool = new ArrayList<>();
    private final HashSet<Integer> removed = new HashSet<>();
    private HashSet<Value> value2Stack = new HashSet<>();
    private BasicBlock currentBlock;
    private int instrPos;

    public RegAlloc(Module module) {
        this.module = module;
    }

    public void regAlloc() {
        for (Function function: module.getFunctions()) {
            value2reg = new HashMap<>();
            value2Stack = new HashSet<>();
            reg2Value.clear();
            freeRegsPool.clear();
            freeRegsPool.addAll(MipsRegister.allocableRegs());
            instrPos = 0;
            // System.out.println("\n" + function.getFullName() + ":\n");
            for (int i = 0; i < function.getArgc(); i++) {
                regAllocForValue(function.getFuncFParams().get(i));
            }
            regAllocForBlock(function.getBasicBlocks().get(0));
            function.setGlobalRegsMap(value2reg);
            function.setValueInStack(value2Stack);
        }
    }

    /**从左到右扫描，每当遇见一个新变量则分配一个寄存器，当一个变量活跃区间结束时回收寄存器
     * @param block 函数
     */
    private void regAllocForBlock(BasicBlock block) {
        currentBlock = block;
        LinkedList<Instruction> instructions = new LinkedList<>(block.getInstructions());
        for (instrPos = 0; instrPos < instructions.size(); instrPos++) {
            Instruction instr = instructions.get(instrPos);
            if (instr instanceof Phi) {
                value2Stack.add(instr);
                continue;
            }
            HashSet<Value> virtual = new HashSet<>(instr.use());
            if (instr.def() != null) {
                virtual.add(instr.def());
            }
            for (Value value: virtual) {
                regAllocForValue(value);
            }
            for (Integer reg: removed) {
                reg2Value.remove(reg);
                freeRegsPool.add(reg);
            }
            removed.clear();
            if (instr instanceof Call) {
                Call call = (Call) instr;
                if (((Call) instr).getCallFunc().isDefine()) {
                    call.setSaveMap(new HashMap<>(reg2Value));
                }
            }
        }
        for (BasicBlock child: block.getDomChild()) {
            HashSet<Integer> free = new HashSet<>();
            HashMap<Integer, Value> oldReg2Value = new HashMap<>(reg2Value);
            HashSet<Integer> oldFreeRegPool = new HashSet<>(freeRegsPool);
            for (Integer reg: reg2Value.keySet()) {
                if (!child.getIns().contains(reg2Value.get(reg))) {
                    free.add(reg);
                }
            }
            for (Integer reg: free) {
                freeRegsPool.add(reg);
                reg2Value.remove(reg);
            }
            regAllocForBlock(child);
            reg2Value.clear();
            reg2Value.putAll(oldReg2Value);
            freeRegsPool.clear();
            freeRegsPool.addAll(oldFreeRegPool);
        }
    }

    private void regAllocForValue(Value value) {
        if (value instanceof Constant || value instanceof GlobalVariable || value instanceof Alloca) {
            return;
        }
        // 需要放到栈上的变量
        if (value2Stack.contains(value) || value instanceof Phi) {
            return;
        }
        // 已经分配寄存器,检查是否可以释放
        if (value2reg.containsKey(value)) {
            if (canFree(value)) {
                int reg = value2reg.get(value);
                removed.add(reg);
            }
            return;
        }
        // 分配寄存器
        if (!freeRegsPool.isEmpty()) {  // 如果寄存器池不为空则直接分配
            int reg = freeRegsPool.remove(0);
            value2reg.put(value, reg);
            reg2Value.put(reg, value);
            // System.out.println(value.getFullName() + ": " + reg);
        } else {    // 无法分配寄存器，需要放到栈上
            value2Stack.add(value);
            // System.out.println(value.getFullName() + ": in stack");
        }
    }

    /**如果该变量是局部变量最后一次使用，则可以释放物理寄存器映射，该物理寄存器可添加进临时寄存器池
     * @param value 变量
     * @return 是否可以释放
     */
    private boolean canFree(Value value) {
        if (currentBlock.getOuts().contains(value)) {
            return false;
        }
        for (int i = instrPos + 1; i < currentBlock.getInstructions().size(); i++) {
            if (currentBlock.getInstructions().get(i).use().contains(value)
                    || value.equals(currentBlock.getInstructions().get(i).def())) {
                return false;
            }
        }
        return true;
    }

}
