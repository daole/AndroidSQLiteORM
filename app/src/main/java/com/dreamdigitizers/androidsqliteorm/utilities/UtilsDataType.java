package com.dreamdigitizers.androidsqliteorm.utilities;

import android.text.TextUtils;

import com.dreamdigitizers.androidsqliteorm.annotations.ForeignKey;
import com.dreamdigitizers.androidsqliteorm.annotations.Table;

import java.lang.reflect.Field;

public class UtilsDataType {
    public static final String DATA_TYPE__INTEGER = "INTEGER";
    public static final String DATA_TYPE__REAL = "REAL";
    public static final String DATA_TYPE__TEXT = "TEXT";
    public static final String DATA_TYPE__NUMERIC = "NUMERIC";
    public static final String DATA_TYPE__BLOB = "BLOB";

    public static String inferColumnDataType(Field pColumnField) {
        Class<?> columnDataType = UtilsReflection.extractEssentialFieldType(pColumnField);
        if (UtilsDataType.isSQLiteIntegerDataType(columnDataType)) {
            return UtilsDataType.DATA_TYPE__INTEGER;
        }

        if (UtilsDataType.isSQLiteRealDataType(columnDataType)) {
            return UtilsDataType.DATA_TYPE__REAL;
        }

        if (UtilsDataType.isSQLiteTextDataType(columnDataType)) {
            return UtilsDataType.DATA_TYPE__TEXT;
        }

        if (UtilsDataType.isSQLiteNumericDataType(columnDataType)) {
            return UtilsDataType.DATA_TYPE__NUMERIC;
        }

        if (UtilsDataType.isSQLiteBlobDataType(columnDataType)) {
            return UtilsDataType.DATA_TYPE__BLOB;
        }

        if (pColumnField.isAnnotationPresent(ForeignKey.class) && columnDataType.isAnnotationPresent(Table.class)) {
            ForeignKey foreignKeyAnnotation = pColumnField.getAnnotation(ForeignKey.class);

            String primaryColumnName = foreignKeyAnnotation.primaryColumnName();
            if (TextUtils.isEmpty(primaryColumnName)) {
                primaryColumnName = UtilsReflection.getColumnName(pColumnField);
            }

            Field primaryColumnField = UtilsReflection.getColumnFieldByColumnName(primaryColumnName, columnDataType);
            if (primaryColumnField == null) {
                throw new RuntimeException(String.format("There is no primaryColumnName '%s' in the table class '%s'.", primaryColumnName, columnDataType.getSimpleName()));
            }

            return UtilsDataType.inferColumnDataType(primaryColumnField);
        }

        throw new RuntimeException("Unrecognized column data type: " + columnDataType.getSimpleName());
    }

    public static boolean isSQLiteIntegerDataType(Class<?> pClass) {
        if (Byte.TYPE.isAssignableFrom(pClass)
                || Byte.class.isAssignableFrom(pClass)
                || Short.TYPE.isAssignableFrom(pClass)
                || Short.class.isAssignableFrom(pClass)
                || Integer.TYPE.isAssignableFrom(pClass)
                || Integer.class.isAssignableFrom(pClass)
                || Long.TYPE.isAssignableFrom(pClass)
                || Long.class.isAssignableFrom(pClass)) {
            return true;
        }
        return false;
    }

    public static boolean isSQLiteRealDataType(Class<?> pClass) {
        if (Float.TYPE.isAssignableFrom(pClass)
                || Float.class.isAssignableFrom(pClass)
                || Double.TYPE.isAssignableFrom(pClass)
                || Double.class.isAssignableFrom(pClass)) {
            return true;
        }
        return false;
    }

    public static boolean isSQLiteTextDataType(Class<?> pClass) {
        if (Character.TYPE.isAssignableFrom(pClass)
                || Character.class.isAssignableFrom(pClass)
                || String.class.isAssignableFrom(pClass)) {
            return true;
        }
        return false;
    }

    public static boolean isSQLiteNumericDataType(Class<?> pClass) {
        if (Boolean.TYPE.isAssignableFrom(pClass)
                || Boolean.class.isAssignableFrom(pClass)
                || java.util.Date.class.isAssignableFrom(pClass)
                || java.sql.Date.class.isAssignableFrom(pClass)) {
            return true;
        }
        return false;
    }

    public static boolean isSQLiteBlobDataType(Class<?> pClass) {
        if (byte[].class.isAssignableFrom(pClass)) {
            return true;
        }
        return false;
    }

    public static boolean isSQLitePrimitiveDataType(Class<?> pClass) {
        if (UtilsDataType.isSQLiteIntegerDataType(pClass)
                || UtilsDataType.isSQLiteRealDataType(pClass)
                || UtilsDataType.isSQLiteTextDataType(pClass)
                || UtilsDataType.isSQLiteNumericDataType(pClass)
                || UtilsDataType.isSQLiteBlobDataType(pClass)) {
            return true;
        }
        return false;
    }
}
