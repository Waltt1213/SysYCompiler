package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;

public class Constant extends Value {
    private boolean isString;

    public Constant(String name) {
        super(new ValueType.Type(ValueType.DataType.Integer32Ty), name);
    }

    public Constant(ValueType.Type type, String name) {
        super(type, name);
    }

    public void setString(boolean string) {
        isString = string;
    }

    public boolean isString() {
        return isString;
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
