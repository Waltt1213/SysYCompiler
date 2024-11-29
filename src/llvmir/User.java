package llvmir;

import java.util.ArrayList;

// 而继承自User的子类意味着，这个实体使用了一个或者多个Value接口。

public class User extends Value {
    protected ArrayList<Value> operands; // 操作数

    public User(ValueType.Type vt, String name) {
        super(vt, name);
        operands = new ArrayList<>();
    }

    public ArrayList<Value> getOperands() {
        return operands;
    }

    public void addOperands(Value value) {
        operands.add(value); // 使用这个value作为操作数
        if (value != null) {
            value.addUser(this); // value被自己使用
        }
    }

    public void removeOperands(Value value) {
        operands.remove(value);
        if (value != null) {
            value.removeUser(this);
        }
    }

    /**将就操作数替换为新操作数
     * @param newValue 新操作数
     * @param oldValue 旧操作数
     */
    public void replaceValue(Value newValue, Value oldValue) {
        for (int i = 0; i < operands.size(); i++) {
            if (operands.get(i).equals(oldValue)) {
                operands.set(i, newValue);
                newValue.addUser(this);
            }
        }
    }

    public void replaceValue(Value newValue, int index) {
        operands.set(index, newValue);
        newValue.addUser(this);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
