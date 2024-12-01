package middle.optimizer;

import llvmir.Module;
import llvmir.User;
import llvmir.Value;
import llvmir.values.BasicBlock;
import llvmir.values.Function;
import llvmir.values.GlobalVariable;
import llvmir.values.instr.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class DCE {
    private Module module;
    private HashSet<Instruction> useful;

    public DCE(Module module) {
        this.module = module;
        useful = new HashSet<>();
    }

    public void dce() {
        delDeadCode();
    }

    public void delDeadCode() {
        initUseful();
        ArrayList<Value> useSet = new ArrayList<>(useful);
        while (!useSet.isEmpty()) {
            Value use = useSet.get(0);
            useSet.remove(use);
            if (use instanceof User) {
                for (Value operand : ((User) use).getOperands()) {
                    if (operand instanceof Instruction && !isUseful((Instruction) operand)
                            && !useful.contains(operand)) {
                        useSet.add(operand);
                        useful.add((Instruction) operand);
                    } else if (operand instanceof GlobalVariable) {
                        useSet.add(operand);
                    }
                }
            }
        }
        for (Function function: module.getFunctions()) {
            for (BasicBlock basicBlock: function.getBasicBlocks()) {
                Iterator<Instruction> iterator = basicBlock.getInstructions().iterator();
                while (iterator.hasNext()) {
                    Instruction instruction = iterator.next();
                    if (useful.contains(instruction)) {
                        continue;
                    }
                    iterator.remove();
                }
            }
        }
    }

    private void initUseful() {
        useful.clear();
        for (Function function: module.getFunctions()) {
            for (BasicBlock basicBlock: function.getBasicBlocks()) {
                for (Instruction instruction: basicBlock.getInstructions()) {
                    if (isUseful(instruction)) {
                        useful.add(instruction);
                    }
                }
            }
        }
    }

    private boolean isUseful(Instruction instruction) {
        return instruction instanceof Branch
                || instruction instanceof Return
                || instruction instanceof Call
                || instruction instanceof Store;
    }
}
