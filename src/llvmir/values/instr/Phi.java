package llvmir.values.instr;

import llvmir.ValueType;
import llvmir.values.BasicBlock;

import java.util.ArrayList;

public class Phi extends Instruction {
    private ArrayList<BasicBlock> preBlocks;

    public Phi(ValueType.Type vt, String name) {
        super(vt, Type.PHI, name);
    }

    public void addPreBlock(BasicBlock basicBlock) {
        preBlocks.add(basicBlock);
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
