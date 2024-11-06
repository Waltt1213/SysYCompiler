package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.instr.Instruction;
import middle.SlotTracker;

import java.util.ArrayList;
import java.util.LinkedList;

public class BasicBlock extends Value {
    private final LinkedList<Instruction> instructions;
    private ArrayList<Label> preds;
    private Function parent;
    private boolean isLabeled;
    private boolean isTerminator;

    public BasicBlock(String name) {
        super(new ValueType.Type(ValueType.DataType.LabelTy), name);
        instructions = new LinkedList<>();
        preds = new ArrayList<>();
    }

    public void setParent(Function parent) {
        this.parent = parent;
    }

    public Function getParent() {
        return parent;
    }

    public void setLabeled(boolean labeled) {
        isLabeled = labeled;
    }

    public boolean isLabeled() {
        return isLabeled;
    }

    public void setName(String name) {
        super.setName(name);
    }

    public void appendInstr(Instruction instr, boolean setName) {
        if (!isTerminator) {
            if (setName) {
                instr.setName(SlotTracker.slot());
            }
            instructions.add(instr);
        }
    }

    public void addPred(Label label) {
        preds.add(label);
    }

    public boolean isTerminator() {
        return isTerminator;
    }

    public void setTerminator(Instruction branch) {
        appendInstr(branch, false);
        isTerminator = true;
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

        public ForBlock(String name, BasicBlock outBlock) {
            super(name);
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
