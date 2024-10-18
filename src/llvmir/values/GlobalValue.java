package llvmir.values;

import llvmir.TypeId;
import llvmir.Value;

public class GlobalValue extends Value {
    public GlobalValue(TypeId vt, String name) {
        super(vt, name);
    }
}
