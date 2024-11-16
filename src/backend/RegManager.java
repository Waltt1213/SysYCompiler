package backend;

import backend.mips.MipsRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegManager {
    private ArrayList<MipsRegister> regPool = new ArrayList<>();
    private HashMap<Integer, String> tempUseMap = new HashMap<>();    // real -> virtual
    private HashMap<Integer, String> argumentUseMap = new HashMap<>();   // argue -> virtual
    private HashMap<Integer, String> restoreMap;

    public RegManager() {
        for (int i = 0; i < 32; i++) {
            regPool.add(new MipsRegister(i));
            tempUseMap.put(i, "");
            argumentUseMap.put(i, "");
        }
    }

    /**
     * @param name 虚拟寄存器名
     */
    public MipsRegister setTempRegUse(String name) {
        for (int i = 8; i < 18; i++) {
            if (tempUseMap.get(i).isEmpty()) {
                tempUseMap.put(i, name);
                return regPool.get(i);
            }
        }
        // 没有空的临时寄存器
        return null;
    }

    /**
     * @param virtualName 虚拟寄存器名称
     * @return  可用的临时寄存器
     */
    public MipsRegister getTempReg(String virtualName) {
        // t0 -> t7, t8, t9
        for (int i = 8; i < 18; i++) {
            if (tempUseMap.get(i).equals(virtualName)) {
                return regPool.get(i);
            }
        }
        return setTempRegUse(virtualName);

    }

    public HashMap<Integer, String> unusedMap() {
        HashMap<Integer, String> unused = new HashMap<>();
        for (Map.Entry<Integer, String> entry: tempUseMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                unused.put(entry.getKey(), entry.getValue());
            }
        }
        setRestoreMap(unused);
        return unused;
    }

    public void setRestoreMap(HashMap<Integer, String> restoreMap) {
        this.restoreMap = restoreMap;
    }

    public HashMap<Integer, String> getRestoreMap() {
        return restoreMap;
    }

    /**
     * @param virtualName 虚拟寄存器名
     * @return 逻辑寄存器
     */
    public MipsRegister getReg(String virtualName) {
        MipsRegister res = getArgueReg(virtualName);
        if (res == null) {
            return getTempReg(virtualName);
        }
        return res;
    }

    public MipsRegister getReg(int no) {
        return regPool.get(no);
    }

    /**
     * @param name 表示函数参数的虚拟寄存器名
     * @param no    逻辑寄存器编号 a0: 4
     */
    public void setArgueRegUse(String name, int no) {
        argumentUseMap.put(no, name);
    }

    /**
     * @param name 虚拟寄存器名
     * @return 参数寄存器
     */
    public MipsRegister getArgueReg(String name) {
        for (Integer no: argumentUseMap.keySet()) {
            if (argumentUseMap.get(no).equals(name)) {
                return regPool.get(no);
            }
        }
        return null;
    }

    public void resetTempReg(MipsRegister reg) {
        if (reg == null) {
            return;
        }
        if (reg.getNo() < 8 || reg.getNo() > 17) {
            return;
        }
        tempUseMap.put(reg.getNo(), "");
    }

    public void clear() {
        for (int i = 0; i < 32; i++) {
            tempUseMap.put(i, "");
            argumentUseMap.put(i, "");
        }
    }

    public HashMap<Integer, String> getTempUseMap() {
        return tempUseMap;
    }
}
