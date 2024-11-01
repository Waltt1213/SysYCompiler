package llvmir;

import java.util.ArrayList;

// 而继承自User的子类意味着，这个实体使用了一个或者多个Value接口。

public class User extends Value {
    protected ArrayList<Value> operands; // 操作数

    public User(DataType vt, String name) {
        super(vt, name);
        operands = new ArrayList<>();
    }

    public ArrayList<Value> getOperands() {
        return operands;
    }

    public void addOperands(Value value) {
        operands.add(value); // 使用这个value作为操作数
        value.addUser(this); // value被自己使用
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
