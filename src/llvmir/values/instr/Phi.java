package llvmir.values.instr;

import llvmir.Value;
import llvmir.values.BasicBlock;
import llvmir.values.Constant;

import java.util.ArrayList;
import java.util.HashSet;

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

    public HashSet<BasicBlock> getPreBlockFromInstr(Instruction instruction) {
        HashSet<Integer> indexs = new HashSet<>();
        if (operands.contains(instruction)) {
            for (int i = 0; i < preBlocks.size(); i++) {
                if (operands.get(i).equals(instruction)) {
                    indexs.add(i);
                }
            }
        }
        HashSet<BasicBlock> fromBlocks = new HashSet<>();
        for (int i = 0; i < preBlocks.size(); i++) {
            if (indexs.contains(i)) {
                fromBlocks.add(preBlocks.get(i));
            }
        }
        return fromBlocks;
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
