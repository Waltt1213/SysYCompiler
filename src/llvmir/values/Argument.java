package llvmir.values;

import llvmir.TypeId;
import llvmir.Value;

public class Argument extends Value {
    public Argument(TypeId vt, String name) {
        super(vt, name);
    }
}
