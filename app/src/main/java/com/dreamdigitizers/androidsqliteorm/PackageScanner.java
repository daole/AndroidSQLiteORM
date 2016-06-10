package com.dreamdigitizers.androidsqliteorm;

import android.content.Context;

import com.dreamdigitizers.androidsqliteorm.annotations.Table;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import dalvik.system.DexFile;

class PackageScanner {
    public static Set<Class<?>> scanRequiredClasses(Context pContext) throws IOException, ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<>();
        DexFile dexFile = new DexFile(pContext.getApplicationInfo().sourceDir);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<String> entries = dexFile.entries();
        while (entries.hasMoreElements()) {
            String entry = entries.nextElement();
            Class<?> clazz = classLoader.loadClass(entry);
            if (PackageScanner.checkIfClassRequired(clazz)) {
                classes.add(clazz);
            }
        }
        return classes;
    }

    private static boolean checkIfClassRequired(Class pClass) {
        boolean result = false;
        if (pClass.isAnnotationPresent(Table.class)) {
            result = true;
        }
        return result;
    }
}
