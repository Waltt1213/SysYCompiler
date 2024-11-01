package llvmir.values;

import llvmir.DataType;
import llvmir.Value;

public class Constant extends Value {
    public Constant(String name) {
        super(DataType.Integer32Ty, name);
    }

    public Constant(DataType type, String name) {
        super(type, name);
    }

    @Override
    public String getFullName() {
        return name;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public String toString() {
        return name;
    }
}
