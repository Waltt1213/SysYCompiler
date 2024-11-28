package llvmir;

import java.util.ArrayList;
import java.util.Objects;

// 一个继承自Value的类意味着，它定义了一个结果，可被其它IR使用。
public class Value {
    protected String name;
    protected ValueType.Type tp;    // 返回值类型
    protected String id = localId;
    public static String globalId = "@";
    public static String localId = "%";
    public static String constId = "#";
    protected ArrayList<User> usersList; // 使用这个value的user

    public Value(ValueType.Type vt, String name) {
        this.name = name;
        this.tp = vt;
        usersList = new ArrayList<>();
    }

    public void addUser(User user) {
        usersList.add(user);
    }

    public void removeUser(User user) {
        usersList.remove(user);
    }

    public ArrayList<User> getUsersList() {
        return usersList;
    }

    public String getName() {
        return name;
    }

    public ValueType.Type getTp() {
        return tp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTp(ValueType.Type tp) {
        this.tp = tp;
    }

    public String getFullName() {
        return id + name;
    }

    public String getId() {
        return id;
    }

    public String getDef() {
        return getTp().toString() + " " + getFullName();
    }

    public void remove() {
        usersList.clear();
    }

    /**将使用旧value作为操作数的user替换旧操作数为新value
     * @param newValue 新的操作数
     */
    public void replaceAllUses(Value newValue) {
        for (User user: usersList) {
            user.replaceValue(newValue, this);
        }
    }

    @Override
    public String toString() {
        return getDef();
    }
}
