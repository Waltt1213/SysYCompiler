package llvmir.values.instr;

import llvmir.DataType;
import llvmir.Value;

public class Return extends Instruction {

    public Return(String name) {
        super(DataType.VoidTy, Type.RET, name);
    }

    public Return(String name, Value value) {
        super(value.getTp(), Type.RET, name);
        addOperands(value);
    }

    @Override
    public String toString() {
        if (tp.equals(DataType.VoidTy)) {
            return "ret void";
        } else {
            return "ret " + getOperands().get(0).getDef();
        }
    }
}
