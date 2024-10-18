package llvmir;

import java.util.ArrayList;

public class Value {
    // private ValueType vt;   // 数据类型
    protected String name;
    protected TypeId tp;    // 返回值类型
    protected ArrayList<User> usersList;
    protected ArrayList<Use> useList;

    public Value(TypeId vt, String name) {
        this.name = name;
        this.tp = vt;
        useList = new ArrayList<>();
        usersList = new ArrayList<>();
    }

}
