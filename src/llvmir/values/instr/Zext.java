package llvmir.values.instr;

import llvmir.DataType;
import llvmir.Value;

public class Zext extends Instruction {

    public Zext(DataType vt, Type irType, String name) {
        super(vt, irType, name);
    }

    @Override
    public String toString() {
        // <result> = zext <ty> <value> to <ty2>
        StringBuilder sb = new StringBuilder();
        Value value1 = getOperands().get(0);
        sb.append(id).append(name).append(" = ");
        sb.append("zext ").append(value1.getDef());
        sb.append(" to ").append(tp);
        return sb.toString();
    }
}
