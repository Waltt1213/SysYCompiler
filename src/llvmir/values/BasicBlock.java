package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.instr.Branch;
import llvmir.values.instr.Instruction;
import middle.SlotTracker;

import java.util.ArrayList;
import java.util.LinkedList;

public class BasicBlock extends Value {
    private final LinkedList<Instruction> instructions;
    private final Function parent;
    private boolean needName = false;   // 需要SlotTracker提供一个名字
    private boolean isLabeled;  // 需要打印出名字
    private boolean isTerminator;
    private ArrayList<BasicBlock> subsequents = new ArrayList<>();
    private ArrayList<BasicBlock> precursor = new ArrayList<>();
    private BasicBlock direct;

    public BasicBlock(String name, Function function) {
        super(new ValueType.Type(ValueType.DataType.LabelTy), name);
        instructions = new LinkedList<>();
        parent = function;
    }

    public void setNeedName(boolean needName) {
        this.needName = needName;
    }

    public boolean isNeedName() {
        return needName;
    }

    public void setLabeled(boolean labeled) {
        isLabeled = labeled;
    }

    public boolean isLabeled() {
        return isLabeled;
    }

    public String getLabel() {
        return "_" + parent.getName() + "_B" + getName();
    }

    public Function getParent() {
        return parent;
    }

    public void setName(String name) {
        super.setName(name);
    }

    public void addPreBlock(BasicBlock basicBlock) {
        precursor.add(basicBlock);  // basicBlock -> this
        basicBlock.addSubBlock(this);
    }

    public void addSubBlock(BasicBlock basicBlock) {
        subsequents.add(basicBlock);
    }

    public void setDirect(BasicBlock basicBlock) {
        direct = basicBlock;
    }

    public BasicBlock getDirect() {
        return direct;
    }

    public void appendInstr(Instruction instr, boolean setName) {
        if (!isTerminator) {
            // if (setName) {
                // instr.setName(SlotTracker.slot());

        // }
            instr.setNeedName(setName);
            instructions.add(instr);
            return;
        }
        for (Value operand: instr.getOperands()) {
            operand.removeUser(instr);
        }
    }

    public void insertInstr(Instruction instr, Instruction value, boolean setName) {
        if (!isTerminator) {
            if (setName) {
                instr.setName(SlotTracker.slot());
            }
            int index = instructions.indexOf(value);
            instructions.add(index + 1, instr);
        }
    }

    public void setTerminator(Instruction branch) {
        appendInstr(branch, false);
        isTerminator = true;
    }

    public void setTerminator(boolean terminator) {
        isTerminator = terminator;
    }

    public boolean isTerminator() {
        return isTerminator;
    }

    public void setVirtualName() {
        for (Instruction instruction: instructions) {
            instruction.setVirtualName();
        }
    }

    public LinkedList<Instruction> getInstructions() {
        return instructions;
    }

    @Override
    public String getDef() {
        return super.getDef();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isLabeled) {
            sb.append(getName()).append(": ");
            sb.append("\n");
        }
        for (Instruction instr: instructions) {
            sb.append("\t").append(instr.toString()).append("\n");
        }
        return sb.toString();
    }

    public static class ForBlock extends BasicBlock {
        private BasicBlock outBlock;
        private BasicBlock updateBlock;

        public ForBlock(String name, BasicBlock outBlock, Function function) {
            super(name, function);
            this.outBlock = outBlock;
        }

        public void setOutBlock(BasicBlock outBlock) {
            this.outBlock = outBlock;
        }

        public void setUpdateBlock(BasicBlock updateBlock) {
            this.updateBlock = updateBlock;
        }

        public BasicBlock getOutBlock() {
            return outBlock;
        }

        public BasicBlock getUpdateBlock() {
            if (updateBlock == null) {
                return this;
            }
            return updateBlock;
        }
    }
}
