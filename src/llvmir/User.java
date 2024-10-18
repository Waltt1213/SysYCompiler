package llvmir;

import java.util.ArrayList;

public class User extends Value {
    protected ArrayList<Value> operands;
    protected ArrayList<Use> useList;

    public User(TypeId vt, String name) {
        super(vt, name);
        operands = new ArrayList<>();
        useList = new ArrayList<>();
    }
}
