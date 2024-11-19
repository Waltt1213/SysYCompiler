package llvmir.values.instr;

import llvmir.ValueType;
import llvmir.values.BasicBlock;
import middle.SlotTracker;

public class Branch extends Instruction {

    public Branch(String name) {
        super(new ValueType.Type(ValueType.DataType.VoidTy), Type.BR, name);
    }

    @Override
    public String toString() {
        if (getOperands().size() == 1) {
            return "br " + getOperands().get(0).getDef();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("br ");
            sb.append(getOperands().get(0).getDef()).append(", ");
            sb.append(getOperands().get(1).getDef()).append(", ");
            sb.append(getOperands().get(2).getDef());
            return sb.toString();
        }
    }
}
