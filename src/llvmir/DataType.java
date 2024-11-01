package llvmir;

public enum DataType {
    // Primitive types
    VoidTy("void"),       //空返回值
    LabelTy("label"),      //标签类型

    // Derived types
    Integer8Ty("i8"),    //8位整数类型
    Integer32Ty("i32"),    //32位整数类型
    Pointer32Ty("i32*"),    //指针类型
    Pointer8Ty("i8*");

    private final String value;

    DataType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
