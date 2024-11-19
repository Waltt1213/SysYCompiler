package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.instr.Branch;
import llvmir.values.instr.Instruction;
import middle.SlotTracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class BasicBlock extends Value {
    private final LinkedList<Instruction> instructions;
    private final Function parent;
    private boolean needName = false;   // 需要SlotTracker提供一个名字
    private boolean isLabeled;  // 需要打印出名字
    private boolean isTerminator;
    private final HashSet<BasicBlock> subsequents = new HashSet<>();
    private final HashSet<BasicBlock> precursor = new HashSet<>();
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
            instr.setNeedName(setName);
            instructions.add(instr);
            if (instr instanceof Branch) {
                Branch branch = (Branch) instr;
                if (branch.getOperands().size() == 1) {
                    ((BasicBlock) branch.getOperands().get(0)).addPreBlock(this);
                } else if (branch.getOperands().size() == 3) {
                    ((BasicBlock) branch.getOperands().get(1)).addPreBlock(this);
                    ((BasicBlock) branch.getOperands().get(2)).addPreBlock(this);
                }
            }
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
        // 优化不必要的条件跳转
        if (branch instanceof Branch && branch.getOperands().size() == 3) {
            Branch br = (Branch) branch;
            Value judge = br.getOperands().get(0);
            Value trueBlock = br.getOperands().get(1);
            Value falseBlock = br.getOperands().get(2);
            if (judge instanceof Constant) {
                if (judge.getName().equals("0")) {  // 永远为假
                    br.removeOperands(trueBlock);
                    br.removeOperands(judge);
                } else {    // 永远为真
                    br.removeOperands(falseBlock);
                    br.removeOperands(judge);
                }
            }
        }
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
        if (!precursor.isEmpty()) {
            sb.append("; pred = ");
            for (BasicBlock basicBlock: precursor) {
                sb.append(basicBlock.getFullName()).append(", ");
            }
            sb.append("\n");
        }
        if (!subsequents.isEmpty()) {
            sb.append("; next = ");
            for (BasicBlock basicBlock: subsequents) {
                sb.append(basicBlock.getFullName()).append(", ");
            }
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
