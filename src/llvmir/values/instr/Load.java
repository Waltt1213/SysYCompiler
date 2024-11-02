package llvmir.values.instr;

import llvmir.ValueType;

public class Load extends Instruction {

    public Load(ValueType.Type vt, String name) {
        super(vt, Type.LOAD, name);
    }

    @Override
    public String toString() {
        return getFullName() + " = load " + tp.toString() + ", "
                + getOperands().get(0).getDef();
    }
}
