package com.dreamdigitizers.androidsqliteorm.utilities;

import android.text.TextUtils;

import com.dreamdigitizers.androidsqliteorm.annotations.ForeignKey;
import com.dreamdigitizers.androidsqliteorm.annotations.Table;

import java.lang.reflect.Field;
import java.util.Date;

public class UtilsDataType {
    public static final String DATA_TYPE__INTEGER = "INTEGER";
    public static final String DATA_TYPE__REAL = "REAL";
    public static final String DATA_TYPE__TEXT = "TEXT";
    public static final String DATA_TYPE__NUMERIC = "NUMERIC";
    public static final String DATA_TYPE__BLOB = "BLOB";

    public static String inferColumnDataType(Field pColumnField) {
        Class<?> columnDataType = pColumnField.getType();
        if (columnDataType.isAssignableFrom(Byte.TYPE)
                || columnDataType.isAssignableFrom(Byte.class)
                || columnDataType.isAssignableFrom(Short.TYPE)
                || columnDataType.isAssignableFrom(Short.class)
                || columnDataType.isAssignableFrom(Integer.TYPE)
                || columnDataType.isAssignableFrom(Integer.class)
                || columnDataType.isAssignableFrom(Long.TYPE)
                || columnDataType.isAssignableFrom(Long.class)) {
            return UtilsDataType.DATA_TYPE__INTEGER;
        }

        if (columnDataType.equals(Float.TYPE)
                || columnDataType.equals(Float.class)
                || columnDataType.equals(Double.TYPE)
                || columnDataType.equals(Double.class)) {
            return UtilsDataType.DATA_TYPE__REAL;
        }

        if (columnDataType.isAssignableFrom(Character.TYPE)
                || columnDataType.isAssignableFrom(Character.class)
                || columnDataType.equals(String.class)) {
            return UtilsDataType.DATA_TYPE__TEXT;
        }

        if (columnDataType.isAssignableFrom(Boolean.TYPE)
                || columnDataType.isAssignableFrom(Boolean.class)
                || columnDataType.isAssignableFrom(Date.class)
                || columnDataType.isAssignableFrom(java.sql.Date.class)) {
            return UtilsDataType.DATA_TYPE__NUMERIC;
        }

        if (columnDataType.isAssignableFrom(byte[].class)) {
            return UtilsDataType.DATA_TYPE__BLOB;
        }

        if (!columnDataType.isPrimitive() && columnDataType.isAnnotationPresent(Table.class) && pColumnField.isAnnotationPresent(ForeignKey.class)) {
            ForeignKey foreignKeyAnnotation = pColumnField.getAnnotation(ForeignKey.class);

            String masterColumnName = foreignKeyAnnotation.referencedColumnName();
            if (TextUtils.isEmpty(masterColumnName)) {
                masterColumnName = UtilsReflection.getColumnName(pColumnField);
            }

            Field masterColumnField = UtilsReflection.getColumnFieldByColumnName(masterColumnName, columnDataType);
            return UtilsDataType.inferColumnDataType(masterColumnField);
        }

        throw new RuntimeException("Unrecognized column data type: " + columnDataType.getSimpleName());
    }
}
