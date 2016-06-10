package com.dreamdigitizers.androidsqliteorm.helpers;

import java.util.Date;

public class HelperDataType {
    public static String inferColumnDataType(Class<?> pColumnDataType) {
        if (pColumnDataType.isAssignableFrom(Byte.TYPE) ||
                pColumnDataType.isAssignableFrom(Byte.class) ||
                pColumnDataType.isAssignableFrom(Short.TYPE) ||
                pColumnDataType.isAssignableFrom(Short.class) ||
                pColumnDataType.isAssignableFrom(Integer.TYPE) ||
                pColumnDataType.isAssignableFrom(Integer.class) ||
                pColumnDataType.isAssignableFrom(Long.TYPE) ||
                pColumnDataType.isAssignableFrom(Long.class)) {
            return "INTEGER";
        }

        if (pColumnDataType.equals(Float.TYPE) ||
                pColumnDataType.equals(Float.class) ||
                pColumnDataType.equals(Double.TYPE) ||
                pColumnDataType.equals(Double.class)) {
            return "REAL";
        }

        if (pColumnDataType.isAssignableFrom(Character.TYPE) ||
                pColumnDataType.isAssignableFrom(Character.class) ||
                pColumnDataType.equals(String.class)) {
            return "TEXT";
        }

        if (pColumnDataType.isAssignableFrom(Boolean.TYPE) ||
                pColumnDataType.isAssignableFrom(Boolean.class) ||
                pColumnDataType.isAssignableFrom(Date.class) ||
                pColumnDataType.isAssignableFrom(java.sql.Date.class)) {
            return "NUMERIC";
        }

        if (pColumnDataType.isAssignableFrom(byte[].class)) {
            return "BLOB";
        }

        throw new RuntimeException("Unrecognized column data type: " + pColumnDataType.getSimpleName());
    }
}
