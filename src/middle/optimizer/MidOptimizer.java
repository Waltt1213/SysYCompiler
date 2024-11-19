package middle.optimizer;

import llvmir.Module;
import llvmir.values.Function;

import java.util.ArrayList;

public class MidOptimizer {
    private Module module;
    private ArrayList<Function> functions;

    public MidOptimizer(Module module) {
        this.module = module;
        functions = module.getFunctions();
    }

    public void optimize() {
        removeDeadBlocks();
    }

    // 优化1：删除死代码块
    private void removeDeadBlocks() {

    }

    public Module getModule() {
        return module;
    }
}
