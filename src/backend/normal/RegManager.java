package backend.normal;

import backend.mips.MipsRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegManager {
    private ArrayList<MipsRegister> regPool = new ArrayList<>();
    private HashMap<Integer, String> tempUseMap = new HashMap<>();    // real -> virtual
    private HashMap<Integer, String> argumentUseMap = new HashMap<>();   // argue -> virtual
    private HashMap<Integer, String> restoreMap;
    private int discard = 8;
    private MipsRegister discardReg = null;
    private MipsRegister isUsing;

    private String discardVirtual;

    private final StackManager stackManager = StackManager.getInstance();

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
                isUsing = regPool.get(i);
                tempUseMap.put(i, name);
                return regPool.get(i);
            }
        }
        // 没有空的临时寄存器
        // 随便选择一个临时寄存器返回
        discardReg = regPool.get(discard);
        while (discardReg.equals(isUsing)) {
            updateDiscard();
            discardReg = regPool.get(discard);
        }
        discardVirtual = tempUseMap.get(discard);
        tempUseMap.put(discard, name);
        // 把旧值扔到栈上,升级为局部变量
        updateDiscard();
        stackManager.push(discardVirtual);
        return null;
    }

    public void updateDiscard() {
        if (discard == 17) {
            discard = 8;
        } else {
            discard++;
        }
    }

    /**
     * @param virtualName 虚拟寄存器名称
     * @return  可用的临时寄存器
     */
    public MipsRegister getTempReg(String virtualName) {
        // t0 -> t7, t8, t9
        for (int i = 8; i < 18; i++) {
            if (tempUseMap.get(i).equals(virtualName)) {
                isUsing = regPool.get(i);
                return regPool.get(i);
            }
        }
        return null;
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
        if (reg.equals(isUsing)) {
            isUsing = null;
        }
    }

    public void clear() {
        for (int i = 0; i < 32; i++) {
            tempUseMap.put(i, "");
            argumentUseMap.put(i, "");
        }
    }

    public int getDiscard() {
        return discard;
    }

    public MipsRegister getDiscardReg() {
        return discardReg;
    }

    public String getDiscardVirtual() {
        return discardVirtual;
    }

    public HashMap<Integer, String> getTempUseMap() {
        return tempUseMap;
    }
}
