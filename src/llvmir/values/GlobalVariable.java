package llvmir.values;

import llvmir.DataType;
import llvmir.Value;

public class GlobalVariable extends Value {
    public GlobalVariable(DataType vt, String name) {
        super(vt, name);
    }
}
