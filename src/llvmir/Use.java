package llvmir;

public class Use {
    // user 引用 value的记录
    private User user;
    private Value value;

    public Use(User user, Value value) {
        this.user = user;
        this.value = value;
    }

}
