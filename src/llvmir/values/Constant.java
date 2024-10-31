package llvmir.values;

import llvmir.DataType;
import llvmir.Value;

public class Constant extends Value {
    public Constant(DataType vt, String name) {
        super(vt, name);
    }
}
