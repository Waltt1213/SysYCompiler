package backend.mips;

import llvmir.ValueType;

public class MipsRegister {
    public static String[] RegName = {
            "$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
            "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
            "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
            "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"
    };

    private String name;

    public MipsRegister(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MipsRegister)) {
            return false;
        }
        MipsRegister o = (MipsRegister) obj;
        return o.name.equals(this.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
