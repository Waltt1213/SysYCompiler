package middle.optimizer;

import llvmir.Module;
import llvmir.User;
import llvmir.Value;
import llvmir.values.BasicBlock;
import llvmir.values.Function;
import llvmir.values.GlobalVariable;
import llvmir.values.instr.*;

import java.util.*;

public class DCE {
    private final Module module;
    private final HashSet<Instruction> useful;

    public DCE(Module module) {
        this.module = module;
        useful = new HashSet<>();
    }

    public void dce() {
        // analysisDeadCode();
        delDeadCode();
        // functionInline();
    }

    private void analysisDeadCode() {
        for (Function function: module.getFunctions()) {
            for (BasicBlock basicBlock: function.getBasicBlocks()) {
                for (Instruction instruction: basicBlock.getInstructions()) {
                    if (instruction instanceof Store) {
                        function.setNoSideEffect(false);
                    }
                    if (instruction instanceof Call) {
                        Function callFunc = ((Call) instruction).getCallFunc();
                        if (!callFunc.isNoSideEffect()) {   // 有副作用
                            function.setNoSideEffect(false);
                        }
                        function.addChild(((Call) instruction));
                    }
                }
            }
        }
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
                    for (Value operand: instruction.use()) {
                        if (operand instanceof Instruction) {
                            Instruction o = (Instruction) operand;
                            o.removeUser(instruction);
                        }
                    }
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
