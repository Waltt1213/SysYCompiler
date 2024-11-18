package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;

import java.util.ArrayList;

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

    public void setTp(ValueType.Type tp) {
        super.setTp(tp);
    }

    @Override
    public String getFullName() {
        return name;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public Constant deepClone() {
        Constant constant = new Constant(this.name);
        constant.setTp(this.tp.deepClone());
        constant.setString(this.isString);
        constant.usersList = new ArrayList<>(usersList);
        return constant;
    }

    @Override
    public String toString() {
        return tp.toString() + " " + name;
    }
}
