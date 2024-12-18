package middle.optimizer;

import llvmir.Module;
import llvmir.values.BasicBlock;
import llvmir.values.Function;

public class Optimizer {
    private final Module module;
    private final CFG cfg;
    private final Mem2reg mem2reg;
    private final DCE dce;
    private final GVN gvn;
    private final GCM gcm;
    private final SimplifyBlock simplifyBlock;
    private final RemovePhi removePhi;
    private final LiveAnalyze activeAnalyze;
    private final RegAlloc regAlloc;

    public Optimizer(Module module) {
        this.module = module;
        simplifyBlock = new SimplifyBlock(module);
        cfg = new CFG(module);
        mem2reg = new Mem2reg(module);
        dce = new DCE(module);
        gvn = new GVN(module);
        gcm = new GCM(module);
        removePhi = new RemovePhi(module);
        activeAnalyze = new LiveAnalyze(module.getFunctions());
        regAlloc = new RegAlloc(module);
    }

    public void optimizeSSA() {
        simplifyBlock.removeDeadBlocks();
        cfg.buildCFG();     // 构建CFG
        mem2reg.buildSSA(); // 实现SSA
        dce.dce();          // 删除死代码
        cfg.buildCFG();
        gvn.gvn();          // GVN
        gcm.gcm();          // GCM
    }

    public void optimizeBackend() {
        activeAnalyze.analyzeActiveVar();   // 活跃变量分析
        regAlloc.regAlloc();                // 线性扫描分配寄存器
        removePhi.removePhi();              // 消除phi
        genNeighbour();
    }

    public void genNeighbour() {
        for (Function function: module.getFunctions()) {
            BasicBlock pre = function.getBasicBlocks().get(0);
            for (int i = 1; i < function.getBasicBlocks().size(); i++) {
                BasicBlock next = function.getBasicBlocks().get(i);
                pre.setNeighbour(next);
                pre = next;
            }
        }
    }
}
