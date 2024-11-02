package llvmir;

public class ValueType {

    public enum DataType {
        // Primitive types
        VoidTy("void"),       //空返回值
        LabelTy("label"),      //标签类型

        // Derived types
        Integer8Ty("i8"),    //8位整数类型
        Integer32Ty("i32"),
        Integer64Ty("i64");    //32位整数类型

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

    public static class Type {
        private final DataType dataType;

        public Type(DataType dataType) {
            this.dataType = dataType;
        }

        public DataType getDataType() {
            return dataType;
        }

        public int getDim() {
            return 0;
        }

        public Type getActType() {
            return this;
        }

        public Type getAddr() {
            return new PointerType(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Type)) {
                return false;
            }
            Type o = (Type) obj;
            return this.dataType == o.dataType;
        }

        @Override
        public String toString() {
            return dataType.toString();
        }
    }

    public static class PointerType extends Type {
        private final Type actType;

        public PointerType(DataType dataType) {
            super(dataType);
            actType = new Type(dataType);
        }

        public PointerType(Type actType) {
            super(actType.dataType);
            this.actType = actType;
        }

        @Override
        public Type getActType() {
            return actType;
        }

        @Override
        public DataType getDataType() {
            return super.getDataType();
        }

        @Override
        public int getDim() {
            return super.getDim();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PointerType)) {
                return false;
            }
            PointerType o = (PointerType) obj;
            return this.actType.equals(o.actType) && this.getDataType().equals(o.getDataType());
        }

        @Override
        public String toString() {
            return actType.toString() + "*";
        }
    }

    public static class ArrayType extends Type {
        private int dim;

        public ArrayType(DataType dataType) {
            super(dataType);
        }

        @Override
        public DataType getDataType() {
            return super.getDataType();
        }

        public void setDim(int dim) {
            this.dim = dim;
        }

        @Override
        public int getDim() {
            return dim;
        }

        @Override
        public Type getAddr() {
            return new ValueType.PointerType(getDataType());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ArrayType)) {
                return false;
            }
            ArrayType o = (ArrayType) obj;
            return this.dim == o.dim && this.getDataType().equals(o.getDataType());
        }

        @Override
        public String toString() {
            return "[" + dim + " x " + super.toString() + "]";
        }
    }
}
