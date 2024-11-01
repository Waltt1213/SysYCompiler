package llvmir.values.instr;

import llvmir.DataType;
import llvmir.values.Constant;

public class Alloca extends Instruction {
    private Constant dim;

    public Alloca(DataType vt, String name) {
        super(vt, Type.ALLOCA, name);
    }

    public void setDim(Constant dim) {
        this.dim = dim;
    }

    public Constant getDim() {
        return dim;
    }

    public String allocType() {
        if (tp == DataType.Pointer8Ty || tp == DataType.Pointer32Ty) {
            return "[" + dim.getName() + " x " +
                    tp.toString().substring(0, tp.toString().length() - 1) + "]";
        }
        return tp.toString();
    }

    @Override
    public String toString() {
        return getFullName() + " = alloca " + allocType();
    }
}
