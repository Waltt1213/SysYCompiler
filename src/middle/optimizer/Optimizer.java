package middle.optimizer;

import llvmir.Module;

public class Optimizer {
    private final CFG cfg;
    private final Mem2reg mem2reg;
    private final DCE dce;
    private final RemovePhi removePhi;
    private final LiveAnalyze activeAnalyze;

    public Optimizer(Module module) {
        cfg = new CFG(module);
        mem2reg = new Mem2reg(module);
        dce = new DCE(module);
        removePhi = new RemovePhi(module);
        activeAnalyze = new LiveAnalyze(module.getFunctions());
    }

    public void optimizeSSA() {
        cfg.buildCFG();     // 构建CFG
        mem2reg.buildSSA(); // 实现SSA
        dce.dce();          // 删除死代码
    }

    public void optimizeBackend() {
        activeAnalyze.analyzeActiveVar();   // 活跃变量分析
        removePhi.removePhi();              // 消除phi
    }
}
