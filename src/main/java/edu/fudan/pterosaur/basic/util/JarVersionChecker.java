package edu.fudan.pterosaur.basic.util;

import java.io.*;
import java.util.*;
import java.util.jar.*;

public class JarVersionChecker {

    // Helper function to check if a JAR contains any class compiled with Java 21 (major version 65)
    public static boolean isUpperVersionJavaClass(String jarPath, int javaVersion){
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        if (isUpperVersionJavaClass(is, javaVersion)) {
                            return true;
                        }else{
                            return false;
                        }
                    }
                }
            }
        }catch (Exception e){
            return false;
        }
        return false;
    }

    // Check the major version of a class file
    private static boolean isUpperVersionJavaClass(InputStream classFileStream, int javaVersion) throws IOException {
        DataInputStream dataStream = new DataInputStream(classFileStream);
        dataStream.skipBytes(6); // Skip the first 6 bytes to reach the major version
        int majorVersion = dataStream.readUnsignedShort();
        return majorVersion >= javaVersion; // 65 indicates Java 21
    }
}