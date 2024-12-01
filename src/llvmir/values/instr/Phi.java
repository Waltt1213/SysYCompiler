package llvmir.values.instr;

import llvmir.Value;
import llvmir.values.BasicBlock;
import llvmir.values.Constant;

import java.util.ArrayList;

public class Phi extends Instruction {
    private ArrayList<BasicBlock> preBlocks = new ArrayList<>();

    public Phi(Value value, String name) {
        super(value.getTp().getInnerType(), Type.PHI, name);
    }

    public void setPreBlocks(ArrayList<BasicBlock> preBlocks) {
        this.preBlocks = preBlocks;
        for (int i = 0; i < preBlocks.size(); i++) {
            operands.add(new Constant(tp, "0"));
        }
    }

    public void replaceValue(Value value, BasicBlock block) {
        int index = preBlocks.indexOf(block);
        if (index >= 0) {
            replaceValue(value, index);
        }
    }

    @Override
    public void setVirtualName() {
        super.setVirtualName();
        setNeedName(false);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFullName()).append(" = phi ").append(tp.toString()).append(" ");
        for (int i = 0; i < preBlocks.size(); i++) {
            sb.append("[ ");
            sb.append(getOperands().get(i).getFullName());
            sb.append(", ").append(preBlocks.get(i).getFullName());
            sb.append(" ]");
            if (i < preBlocks.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
