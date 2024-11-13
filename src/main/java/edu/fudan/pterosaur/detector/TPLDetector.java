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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: luca
 * Date: 2024/10/17
 * Description:
 */


@Component
public class TPLDetector {

    @Autowired
    GeneralList generalList;

    public void detect() {
        Collection<SootClass> classSnapshot = new ArrayList<>(Scene.v().getApplicationClasses());
        classSnapshot.removeIf(sootClass ->
                generalList.targetPackages.stream().anyMatch(targetPackage ->
                        sootClass.getPackageName().startsWith(targetPackage) && !sootClass.getName().endsWith("Test")
                )
        );
        System.out.println("classSnapshot size is : " + classSnapshot.size());
        List<String> list = new CopyOnWriteArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        for (SootClass sootClass : classSnapshot) {
            detectThirdPartyCalls(sootClass, list);
            // 每处理完一个 SootClass，计数器加1
            int count = counter.incrementAndGet();
            System.out.println("Processed SootClass count: " + count+ " : " + sootClass.getName());
        }

        try (FileWriter writer = new FileWriter("output/tpl.txt")) {
            for (String str : list) {
                writer.write(str + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void detectThirdPartyCalls(SootClass appClass, List list) {
        try {
            Iterator<SootMethod> methodIterator = appClass.getMethods().iterator();
            while (methodIterator.hasNext()) {
                SootMethod method = methodIterator.next();
                Body body;
                try {
                    body = method.retrieveActiveBody();
                } catch (Exception e) {
                    continue;
                }
                for (Unit unit : body.getUnits()) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
                        SootMethod calledMethod = invokeExpr.getMethod();

                        // 获取方法所属类的包名
                        String packageName = calledMethod.getDeclaringClass().getPackageName();
                        if (isThirdPartyPackage(packageName) && isTargetPackage(packageName)) {

                            // 遍历调用方法的参数
                            StringBuilder calledMethodSignature = new StringBuilder(calledMethod.getSignature());
                            List<Type> parameterTypes = calledMethod.getParameterTypes();
                            for (int i = 0; i < parameterTypes.size(); i++) {
                                Type paramType = parameterTypes.get(i);
                                if (paramType instanceof RefType) {
                                    SootClass paramClass = ((RefType) paramType).getSootClass();

                                    // 创建一个标签变量，用于累积所有满足条件的标签
                                    StringBuilder label = new StringBuilder(paramClass.getName());

                                    if (!paramClass.getName().startsWith("java.")) { // 忽略 java 标准库类型
                                        // 判断该参数类型是否是枚举
                                        if (paramClass.isEnum() && !label.toString().contains("-Enum")) {
                                            label.append("-Enum");
                                        }
                                        // 判断该参数类型是否是接口
                                        if (paramClass.isInterface() && !label.toString().contains("-InterfaceClass")) {
                                            label.append("-InterfaceClass");
                                        }
                                        // 判断该参数类型是否是超类
                                        if (paramClass.hasSuperclass() && !label.toString().contains("-SuperClass")) {
                                            label.append("-SuperClass");
                                        }
                                    }

                                    // 在输出中替换原始参数类型为带有标签的类型
                                    calledMethodSignature = new StringBuilder(calledMethodSignature.toString()
                                            .replace(paramClass.getName(), label.toString()));
                                    System.out.println("Method: " + method.getSignature() +
                                            " calls third-party method with tagged parameter: " + calledMethodSignature);
                                }
                            }

                            // 输出修改后的信息
                            System.out.println("Method: " + method.getSignature() +
                                    " calls third-party method: " + calledMethodSignature);
                            list.add(method.getSignature() + "--->" + calledMethodSignature);
                        }

                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isThirdPartyPackage(String packageName) {
        for (String pn : generalList.tplPackages) {
            if (packageName.startsWith(pn)) {
                return true;
            }
        }

        for (String pn : generalList.appPackages) {
            if (!packageName.startsWith(pn) && !packageName.startsWith("java.")&& !packageName.startsWith("javax.") && !packageName.startsWith("soot.") && !packageName.startsWith("org.springframework") && !packageName.startsWith("io.swagger")
                    && !packageName.startsWith("org.slf4j") && !packageName.startsWith("com.github.pagehelper")) {
                return true;
            }
        }
        return false;
    }

    private boolean isTargetPackage(String packageName) {
        if (generalList.targetPackages == null || generalList.targetPackages.size() == 0) {
            return true;
        }
        for (String pn : generalList.targetPackages) {
            if (packageName.startsWith(pn)) {
                return true;
            }
        }
        return false;
    }
}
