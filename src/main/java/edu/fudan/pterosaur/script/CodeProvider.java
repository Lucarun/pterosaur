package edu.fudan.pterosaur.script;

import edu.fudan.pterosaur.basic.cg.CG;
import edu.fudan.pterosaur.basic.common.SootInit;
import edu.fudan.pterosaur.basic.util.SignatureProcessor;
import soot.*;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

import java.io.*;
import java.util.*;

/**
 * User: luca
 * Date: 2024/11/26
 * Description:
 */
public class CodeProvider {

    public static void main(String[] args) {

        String filePath = "/Users/luca/dev/2025/pterosaur/output/popular-components/amqp-client/output/tpl_sort.txt";
        String inputClassPath = "/Users/luca/dev/2025/pterosaur/output/popular-components/amqp-client/downstream";

        SootInit.setSoot_inputClass(Collections.singletonList(inputClassPath), true);


        // 读取签名并处理
        Map<String, List<String>> map = SignatureProcessor.processSignatures(filePath);
        // 遍历 map 按照插入顺序
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            List<String> valueList = entry.getValue();
            // 打印 key
            System.out.println("Key: " + key);
            // 遍历 list，打印其中的每一个 value
            List<SootMethod> entries = new ArrayList<>();
            for (String value : valueList) {
                SootMethod targetMethod = Scene.v().getMethod(value);
                if (targetMethod != null) {
                    entries.add(targetMethod);
                }
            }

            CG cg = new CG(entries);
            System.out.println("cg size : " + cg.callGraph.size());

            for (String value : valueList) {
                System.out.println("Value: " + value);
                Map<SootMethod, List<SootMethod>> methodCallChains = new HashMap<>();
                extracted(value, methodCallChains);
                // 打印分析结果
                methodCallChains.forEach((method, calls) -> {
                    System.out.println("Method: " + method.getSignature());
                    System.out.println("Calls:");
                    calls.forEach(called -> System.out.println("\t" + called.getSignature()));
                });

            }
        }
    }

    private static void extracted(String sig, Map<SootMethod, List<SootMethod>> methodCallChains) {
        SootMethod targetMethod = Scene.v().getMethod(sig);
        // 生成调用图
        //CHATransformer.v().transform();
        // 存储方法体的结果
        analyzeMethod(targetMethod, 3, methodCallChains);
    }

    /**
     * 分析指定方法的调用链，并存储结果
     *
     * @param method           当前方法
     * @param depth            剩余递归深度
     * @param methodCallChains 调用链数据结构
     */
    private static void analyzeMethod(SootMethod method, int depth, Map<SootMethod, List<SootMethod>> methodCallChains) {
        if (depth <= 0 || method == null || !method.hasActiveBody()) return;

        List<SootMethod> calledMethods = new ArrayList<>();
        methodCallChains.put(method, calledMethods);

        CallGraph cg = Scene.v().getCallGraph();
        Iterator<Edge> edges = cg.edgesOutOf(method);

        while (edges.hasNext()) {
            SootMethod callee = edges.next().getTgt().method();
            calledMethods.add(callee);
            analyzeMethod(callee, depth - 1, methodCallChains);
        }
    }


    private static Set<String> processSignatures(String inputFile){
        // 读取文件并处理签名
        Set<String> uniqueSignatures = new LinkedHashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // 提取 < 开始到 <--- 为止的部分作为签名
                int startIdx = line.indexOf('<');
                int endIdx = line.indexOf("<---");

                if (startIdx != -1 && endIdx != -1) {
                    String signature = line.substring(startIdx, endIdx).trim();

                    // 处理签名中的 -InterfaceClass 和 -SuperClass
                    signature = signature.replaceAll("-InterfaceClass|-SuperClass", "");

                    // 添加到 Set 中去重
                    uniqueSignatures.add(signature);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return uniqueSignatures;
    }

    private static void writeSignaturesToFile(Set<String> uniqueSignatures, String outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String signature : uniqueSignatures) {
                writer.write(signature);
                writer.newLine();
            }
        }
    }


    private static void excludeJDKLibrary()
    {
        //exclude jdk classes
        Options.v().set_exclude(excludeList());
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
    }

    private static List<String> excludeList() {
        LinkedList<String> excludeList = new LinkedList<String> (); // 扩展的基本函数package
        excludeList.add("java.");
        excludeList.add("javax.");
        return excludeList;
    }

    private CallGraph constructCG(List<SootMethod> entryPoints){
        releaseCallgraph();
        excludeJDKLibrary();
        Scene.v().setEntryPoints(entryPoints);
        PackManager.v().runPacks();
        return Scene.v().getCallGraph();
    }

    protected void releaseCallgraph() {
        Scene.v().releaseCallGraph();
        Scene.v().releasePointsToAnalysis();
        Scene.v().releaseReachableMethods();
        G.v().resetSpark();
    }

}
