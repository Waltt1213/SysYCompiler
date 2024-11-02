package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;

public class Constant extends Value {
    public Constant(String name) {
        super(new ValueType.Type(ValueType.DataType.Integer32Ty), name);
    }

    public Constant(ValueType.Type type, String name) {
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
        return tp.toString() + " " + name;
    }
}
