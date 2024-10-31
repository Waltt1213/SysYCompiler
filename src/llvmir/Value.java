package llvmir;

import java.util.ArrayList;

// 一个继承自Value的类意味着，它定义了一个结果，可被其它IR使用。
public class Value {
    protected String name;
    protected DataType tp;    // 返回值类型
    protected String id = localId;
    public static String globalId = "@";
    public static String localId = "%";
    protected ArrayList<User> usersList; // 使用这个value的user

    public Value(DataType vt, String name) {
        this.name = name;
        this.tp = vt;
        usersList = new ArrayList<>();
    }

    public void addUser(User user) {
        usersList.add(user);
    }

    public String getName() {
        return name;
    }

    public DataType getTp() {
        return tp;
    }

    @Override
    public String toString() {
        return tp.toString() + " " + id + name;
    }
}
