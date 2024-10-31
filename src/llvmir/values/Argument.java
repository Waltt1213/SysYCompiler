package llvmir.values;

import llvmir.DataType;
import llvmir.Value;

public class Argument extends Value {
    public Argument(DataType vt, String name) {
        super(vt, name);
    }

    @Override
    public String toString() {
        return tp.toString() + " %" + name;
    }
}
