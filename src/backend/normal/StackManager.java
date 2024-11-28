package backend.normal;

import java.util.HashMap;
import java.util.HashSet;

public class StackManager {
    private static StackManager stackManager = new StackManager();
    private int stackPtr;   // 栈顶偏移指针
    private HashMap<String, Integer> stackFrameMap; // 栈帧
    private HashSet<String> globalDataSet = new HashSet<>();

    /**
     * ra <br>
     * fp <br>
     * local variables(局部变量) <br>
     * save register <br>
     * arguments <br>
     */
    private StackManager() {
        stackPtr = 0;
        stackFrameMap = new HashMap<>();
    }

    public static StackManager getInstance() {
        return stackManager;
    }

    public void addGlobalData(String data) {
        globalDataSet.add(data);
    }

    public boolean isGlobalData(String data) {
        return globalDataSet.contains(data);
    }

    /**
     * @param name 虚拟寄存器名称
     * @param size 占内存大小
     * @return 在栈中的位置
     */
    public int putVirtualReg(String name, int size) {
        int res = stackPtr;
        stackFrameMap.put(name, stackPtr);
        stackPtr += size;
        return res;
    }

    public void setVirtualReg(String name, int ptr) {
        stackFrameMap.put(name, ptr);
    }

    public int getStackPtr() {
        return stackPtr;
    }

    public void addPtr(int size) {
        stackPtr += size;
    }

    public int getVirtualPtr(String name) {
        if (!stackFrameMap.containsKey(name)) {
            return -1;
        }
        return stackFrameMap.get(name);
    }

    public boolean inStack(String name) {
        return stackFrameMap.containsKey(name);
    }

    public void push(String name) {
        for (String virtual: stackFrameMap.keySet()) {
            if (stackFrameMap.get(virtual) >= 0) {
                int ptr = stackFrameMap.get(virtual) + 4;
                stackFrameMap.put(virtual, ptr);
            }
        }
        stackFrameMap.put(name, 0);
        stackPtr += 4;
    }

    public void clear() {
        stackPtr = 0;
        stackFrameMap.clear();
    }

}
