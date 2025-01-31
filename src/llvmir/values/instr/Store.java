package llvmir.values.instr;

import llvmir.Value;
import llvmir.ValueType;

public class Store extends Instruction {

    public Store(ValueType.Type vt, String name) {
        super(vt, Type.STORE, name);
    }

    public Value def() {
        return null;
    }

    @Override
    public String toString() {
        return "store " + getOperands().get(0).getDef() + ", "
                + getOperands().get(1).getDef();
    }
}
