package edu.fudan.pterosaur.detector;

import edu.fudan.pterosaur.configuration.GeneralList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: luca
 * Date: 2024/10/17
 * Description:
 */


@Component
public class TPLDetector {

    @Autowired
    GeneralList generalList;

    public void detect(){
        Collection<SootClass> classSnapshot = new ArrayList<>(Scene.v().getApplicationClasses());

        List<String> list = new ArrayList<>();
        for(SootClass sootClass : classSnapshot) {
            detectThirdPartyCalls(sootClass, list);
        }

        try (FileWriter writer = new FileWriter("output/tpl.txt")) {
            for (String str : list){
                writer.write(str + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void detectThirdPartyCalls(SootClass appClass, List list) {
        for (SootMethod method : appClass.getMethods()) {
            Body body;
            try{
                body = method.retrieveActiveBody();
            }catch (Exception e){
                continue;
            }
            for (Unit unit : body.getUnits()) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invokeExpr = stmt.getInvokeExpr();
                    SootMethod calledMethod = invokeExpr.getMethod();

                    // 检测是否调用了第三方库方法
                    String packageName = calledMethod.getDeclaringClass().getPackageName();
                    if (isThirdPartyPackage(packageName) && isTargetPackage(packageName)) {
                        System.out.println("Method: " + method.getSignature() +
                                " calls third-party method: " + calledMethod.getSignature());
                        list.add(method.getSignature() + "--->" + calledMethod.getSignature());
                    }
                }
            }
        }
    }

    private boolean isThirdPartyPackage(String packageName) {
        for (String pn : generalList.tplPackages){
            if (packageName.startsWith(pn)){
                return true;
            }
        }

        for (String pn : generalList.appPackages){
            if (!packageName.startsWith(pn) && !packageName.startsWith("java.") && !packageName.startsWith("soot.") && !packageName.startsWith("org.springframework")&& !packageName.startsWith("io.swagger")
                    && !packageName.startsWith("org.slf4j")&& !packageName.startsWith("com.github.pagehelper")){
                return true;
            }
        }
        return false;
    }

    private boolean isTargetPackage(String packageName) {
        if (generalList.targetPackages ==null || generalList.targetPackages.size() == 0){
            return true;
        }
        for (String pn : generalList.targetPackages){
            if (packageName.startsWith(pn)){
                return true;
            }
        }
        return false;
    }
}
