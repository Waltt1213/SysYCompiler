package llvmir.values;

import llvmir.DataType;
import llvmir.Value;

public class BasicBlock extends Value {
    public BasicBlock(DataType vt, String name) {
        super(vt, name);
    }
}
