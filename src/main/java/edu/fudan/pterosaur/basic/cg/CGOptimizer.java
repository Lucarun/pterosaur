package edu.fudan.pterosaur.basic.cg;

import basic.cfg.CFG;
import basic.interfaces.Singleton;
import lombok.extern.slf4j.Slf4j;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 对于CG中缺失的调用边，尝试通过CFG来补全
 * 只针对对特定方法集合(targetMethods)内方法的调用进行补全
 * <a href="https://vwptc6ky5dr.feishu.cn/docx/KXR3dn0fzoBem1xdxjDcItGGnNg?from=from_copylink">CG优化记录</a>
 */

@Slf4j
public class CGOptimizer implements Singleton {
    public static List<SootMethod> targetMethods;
    public static CG cg;

    // 所有优化过的方法
    public HashSet<SootMethod> optimizedMethods = new HashSet<>();

    public CGOptimizer(List<SootMethod> targetMethods, CG cg) {
        this.targetMethods = targetMethods;
        this.cg = cg;
    }

    public void optimizeCG(CG cg) {
        Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();
        for(SootClass sootClass : applicationClasses) {
            if(sootClass.isJavaLibraryClass()) continue;
            List<SootMethod> methods = new ArrayList<>(sootClass.getMethods());
            for(SootMethod sootMethod : methods) {
                CFG cfg = new CFG(sootMethod, true).buildCFG();
                optimizeMethod(sootMethod, cfg);
            }
        }
    }

    public void optimizeMethod(SootMethod method, CFG cfg) {
        for(Node node : cfg.allNodes.values()) {
            Stmt stmt = (Stmt) node.unit;
            if(stmt.containsInvokeExpr() && cg.calleeOutOf(node.unit).isEmpty()) {
                SootMethod invokeMethod = stmt.getInvokeExpr().getMethod();
                if(targetMethods.contains(invokeMethod)) {
                    Edge edge = new Edge(method, stmt, invokeMethod);
                    cg.callGraph.addEdge(edge);
                    optimizedMethods.add(method);
//                    log.info("optimize method: " + method + " add edge: " + edge);
                }
            }
        }
    }

    private static class SingletonHolder {
        private static final CGOptimizer INSTANCE = new CGOptimizer(targetMethods, cg);
    }

    public static CGOptimizer getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
