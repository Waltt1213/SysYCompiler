package llvmir.values.instr;

import llvmir.Value;
import llvmir.ValueType;

public class Trunc extends Instruction {
    public Trunc(ValueType.Type vt, String name) {
        super(vt, Type.TRUNC, name);
    }

    @Override
    public String toString() {
        // <result> = zext <ty> <value> to <ty2>
        StringBuilder sb = new StringBuilder();
        Value value1 = getOperands().get(0);
        sb.append(id).append(name).append(" = ");
        sb.append("trunc ").append(value1.getDef());
        sb.append(" to ").append(tp);
        return sb.toString();
    }
}
