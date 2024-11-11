package backend;

import backend.mips.MipsRegister;

import java.util.ArrayList;
import java.util.HashMap;

public class RegManager {
    public ArrayList<MipsRegister> regPool = new ArrayList<>();
    public HashMap<Integer, String> tempUseMap = new HashMap<>();    // real -> virtual
    public HashMap<Integer, String> argumentUseMap = new HashMap<>();   // argue -> virtual

    public RegManager() {
        for (int i = 0; i < 32; i++) {
            regPool.add(new MipsRegister(i));
            tempUseMap.put(i, "");
        }
    }

    /**
     * @param virtualName 虚拟寄存器名称
     * @return  可用的临时寄存器
     */
    public MipsRegister getRealTempReg(String virtualName) {
        // t0 -> t7, t8, t9
        for (int i = 8; i < 18; i++) {
            if (tempUseMap.get(i).isEmpty()) {
                tempUseMap.put(i, virtualName);
                return regPool.get(i);
            }
        }
        return null;
    }

    /**
     * @param name 表示函数参数的虚拟寄存器名
     * @param no    逻辑寄存器编号 a0: 4
     */
    public void setArgueRegUse(String name, int no) {
        argumentUseMap.put(no, name);
    }

    public MipsRegister getArgueReg(String name) {
        for (Integer no: argumentUseMap.keySet()) {
            if (argumentUseMap.get(no).equals(name)) {
                return regPool.get(no);
            }
        }
        return null;
    }

    public void resetTempReg(MipsRegister reg) {
        tempUseMap.put(reg.getNo(), "");
    }

    public void clear() {
        for (int i = 0; i < 32; i++) {
            tempUseMap.put(i, "");
        }
    }
}
