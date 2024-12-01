package backend.mips;

import java.util.ArrayList;
import java.util.List;

public class MipsRegister {
    public static String[] RegName = {  // t8 t9的编号与mips原设计有出入
            "$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
            "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
            "$t8", "$t9",
            "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
            "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"
    };

    private final String name;
    private int no;

    public MipsRegister(String name) {
        this.name = name;
        for (int i = 0; i < RegName.length; i++) {
            if (RegName[i].equals(name)) {
                this.no = i;
            }
        }
    }

    public static List<Integer> allocableRegs() {
        List<Integer> regs = new ArrayList<>();
        for (int i = 8; i < 26; i++) {
            if (i != 16 && i != 17) {   // $t8 $t9 留下
                regs.add(i);
            }
        }
        return regs;
    }

    public MipsRegister(int i) {
        this.name = RegName[i];
        this.no = i;
    }

    public String getName() {
        return name;
    }

    public int getNo() {
        return no;
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
