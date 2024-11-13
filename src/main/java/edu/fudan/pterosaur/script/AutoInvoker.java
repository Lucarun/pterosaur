package edu.fudan.pterosaur.script;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class AutoInvoker {

    public static void main(String[] args) {
        String[] jarFilePaths = {
                "/Users/luca/dev/2025/pterosaur/lib/rosefinch-0.0.1-SNAPSHOT.jar",
                "/Users/luca/dev/2025/pterosaur/lib/rosefinch-0.0.1-SNAPSHOT-tests.jar",
        };

        instrument(jarFilePaths);
        invoke(jarFilePaths);
    }

    private static void instrument(String[] jarFilePaths) {
        // 创建ClassPool并将JAR文件添加到ClassPool
        ClassPool pool = ClassPool.getDefault();
        try {
            for (int i = 0; i < jarFilePaths.length; i++) {
                pool.appendClassPath(jarFilePaths[i]); // 加载JAR文件路径
            }
            // 处理JAR文件中的所有类
            processJarFile(jarFilePaths[1], pool);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void processJarFile(String jarFilePath, ClassPool pool) {
        File jarFile = new File(jarFilePath);
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            // 创建临时JAR文件输出流
            File tempJarFile = new File(jarFile.getParent(), "temp_" + jarFile.getName());
            try (JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(tempJarFile))) {
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(".class")) {
                        String className = name.replace('/', '.').replace(".class", "");
                        System.out.println("Processing class: " + className);
                        try {
                            CtClass ctClass = pool.get(className);
                            // 查找带有 @Test 注解的方法并修改其body
                            findTestMethods(ctClass);
                            // 将修改后的字节码写回到临时JAR文件中
                            writeModifiedClassToJar(ctClass, className, outputStream);
                        } catch (NotFoundException e) {
                            // 如果类找不到，跳过
                            System.out.println("Class not found: " + name);
                        }
                    } else {
                        // 其他非.class文件直接复制到新的JAR文件
                        copyJarEntryToNewJar(entry, jar, outputStream);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 删除原JAR并重命名临时文件
            if (jarFile.delete()) {
                tempJarFile.renameTo(jarFile);
            } else {
                System.out.println("Failed to replace original JAR file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeModifiedClassToJar(CtClass ctClass, String className, JarOutputStream outputStream) {
        try {
            // 转换为字节数组并写入到JAR输出流
            byte[] classBytes = ctClass.toBytecode();
            JarEntry entry = new JarEntry(className.replace('.', '/') + ".class");
            outputStream.putNextEntry(entry);
            outputStream.write(classBytes);
            outputStream.closeEntry();
        } catch (IOException | CannotCompileException e) {
            e.printStackTrace();
        }
    }

    private static void copyJarEntryToNewJar(JarEntry entry, JarFile jar, JarOutputStream outputStream) {
        try (InputStream input = jar.getInputStream(entry)) {
            JarEntry newEntry = new JarEntry(entry.getName());
            outputStream.putNextEntry(newEntry);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void findTestMethods(CtClass ctClass) {
        try {
            // 遍历类中的所有方法
            for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
                // 如果方法有 @Test 注解，则修改方法体
                if (ctMethod.hasAnnotation(Test.class)) {
                    System.out.println("Found @Test method: " + ctMethod.getName());
                    modifyMethod(ctMethod);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void modifyMethodBody(CtMethod ctMethod) throws CannotCompileException {
        // 修改方法体，将其设置为输出 "Luca updated"
        ctMethod.setBody("{ System.out.println(\"Luca updated\"); }");
    }


    private static void modifyMethod(CtMethod ctMethod) throws CannotCompileException {
        // 使用 ExprEditor 修改方法调用表达式
        ctMethod.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                CtClass[] parameterTypes;
                try {
                    parameterTypes = m.getMethod().getParameterTypes();
                } catch (NotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                StringBuilder printCode = new StringBuilder();

                // 遍历所有参数，检查是否是接口类型
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i].isInterface()) {
                        // 生成打印语句：输出接口类型参数的具体实现类
                        printCode.append("System.out.println(\"Parameter ")
                                .append(i + 1)
                                .append(" in method ")
                                .append(m.getMethodName())
                                .append(" is an interface. Actual implementation: \" + $")
                                .append(i + 1)
                                .append(".getClass().getName());\n");
                    }
                }

                // 插入代码
                try {
                    if (m.getMethod().getReturnType().equals(CtClass.voidType)) {
                        // 如果方法没有返回值，直接插入打印语句，并使用 $proceed() 保留原方法调用
                        if (printCode.length() > 0) {
                            String newCode = printCode + "$proceed($$);";  // $$ 表示保留原参数
                            m.replace(newCode);
                        }
                    } else {
                        // 如果方法有返回值，则使用 $_ 来接收返回值
                        if (printCode.length() > 0) {
                            String newCode = printCode + "$_ = $proceed($$);";  // $_ 表示返回值
                            m.replace(newCode);
                        }
                    }
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private static void invoke(String[] jarFilePaths) {
        // 创建一个URLClassLoader来加载所有JAR文件
        URL[] urls = new URL[jarFilePaths.length];
        for (int i = 0; i < jarFilePaths.length; i++) {
            try {
                urls[i] = new URL("jar:file:" + jarFilePaths[i] + "!/");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        // 注意：这里我们传递了urls数组和父类加载器（通常是系统类加载器）
        try (URLClassLoader classLoader = new URLClassLoader(urls, AutoInvoker.class.getClassLoader())) {
            for (String jarFilePath : jarFilePaths) {
                processJarFile(jarFilePath, classLoader);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processJarFile(String jarFilePath, URLClassLoader classLoader) {
        try (JarFile jarFile = new JarFile(new File(jarFilePath))) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    System.out.println("className: " + className);
                    try {
                        // 使用类加载器加载类
                        if (className.endsWith("Test")){
                            Class<?> clazz = Class.forName(className, true, classLoader);
                            executeTestMethods(clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        System.out.println("Class not found: " + className);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeTestMethods(Class<?> clazz) {
        // 获取类中的所有方法
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            // 设置方法可访问，即使它是 private 或 protected
            method.setAccessible(true);
            // 获取方法上的所有注解
            Annotation[] annotations = method.getDeclaredAnnotations();
            // 遍历所有注解
            for (Annotation annotation : annotations) {
                // 检查注解是否是@Test
                if (annotation instanceof Test) {
                    System.out.println("Found test method: " + method.getName() + " in class " + clazz.getName());
                    try {
                        method.invoke(clazz.getDeclaredConstructor().newInstance()); // 创建实例并执行方法
                        break;
                    }catch (Exception e){
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
    }
}