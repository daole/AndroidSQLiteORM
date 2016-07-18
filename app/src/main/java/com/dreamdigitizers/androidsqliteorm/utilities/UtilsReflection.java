package com.dreamdigitizers.androidsqliteorm.utilities;

import android.content.Context;
import android.text.TextUtils;

import com.dreamdigitizers.androidsqliteorm.annotations.Column;
import com.dreamdigitizers.androidsqliteorm.annotations.ForeignKey;
import com.dreamdigitizers.androidsqliteorm.annotations.PrimaryKey;
import com.dreamdigitizers.androidsqliteorm.annotations.Relationship;
import com.dreamdigitizers.androidsqliteorm.annotations.Table;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;

public class UtilsReflection {
    public static List<Class<?>> getTableClasses(Context pContext) throws IOException, ClassNotFoundException {
        List<Class<?>> tableClasses = new ArrayList<>();
        DexFile dexFile = new DexFile(pContext.getApplicationInfo().sourceDir);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<String> entries = dexFile.entries();
        while (entries.hasMoreElements()) {
            String entry = entries.nextElement();
            Class<?> clazz = classLoader.loadClass(entry);
            if (UtilsReflection.isTableClass(clazz)) {
                tableClasses.add(clazz);
            }
        }
        return tableClasses;
    }

    public static List<Field> getAllColumnFields(Class<?> pTableClass) {
        List<Field> columnFields = new ArrayList<>();

        Class<?> superClass = pTableClass.getSuperclass();
        if (superClass != null) {
            List<Field> parentColumnFields = UtilsReflection.getAllColumnFields(superClass);
            if (!parentColumnFields.isEmpty()) {
                columnFields.addAll(parentColumnFields);
            }
        }

        Field[] declaredFields = pTableClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (UtilsReflection.isColumnField(declaredField)) {
                columnFields.add(declaredField);
            }
        }

        return columnFields;
    }

    public static List<Field> getAllSelectableColumnFields(Class<?> pTableClass) {
        List<Field> selectableColumnFields = new ArrayList<>();

        Class<?> superClass = pTableClass.getSuperclass();
        if (superClass != null) {
            List<Field> parentSelectableColumnFields = UtilsReflection.getAllSelectableColumnFields(superClass);
            if (!parentSelectableColumnFields.isEmpty()) {
                selectableColumnFields.addAll(parentSelectableColumnFields);
            }
        }

        Field[] declaredFields = pTableClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (UtilsReflection.isSelectableColumnField(declaredField)) {
                selectableColumnFields.add(declaredField);
            }
        }

        return selectableColumnFields;
    }

    public static Field findColumnFieldByColumnName(String pColumnName, Class<?> pTableClass) {
        List<Field> columnFields = UtilsReflection.getAllColumnFields(pTableClass);
        for (Field columnField : columnFields) {
            String columnName = UtilsReflection.getColumnName(columnField);
            if (TextUtils.equals(pColumnName, columnName)) {
                return columnField;
            }
        }
        return null;
    }

    public static Field findPrimaryColumnField(Class<?> pTableClass) {
        List<Field> columnFields = UtilsReflection.getAllColumnFields(pTableClass);
        for (Field columnField : columnFields) {
            if (UtilsReflection.isPrimaryField(columnField)) {
                return columnField;
            }
        }
        return null;
    }

    public static Class<?> extractEssentialFieldType(Field pField) {
        Class<?> clazz = pField.getType();
        if (clazz.isArray()) {
            clazz = clazz.getComponentType();
        } else if (Collection.class.isAssignableFrom(clazz)) {
            ParameterizedType parameterizedType = (ParameterizedType) pField.getGenericType();
            clazz = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        }
        return clazz;
    }

    public static TableInformation getTableInformation(Class<?> pTableClass) {
        TableInformation tableInformation = null;
        if (pTableClass.isAnnotationPresent(Table.class)) {
            Table tableAnnotation = pTableClass.getAnnotation(Table.class);
            tableInformation = new TableInformation();
            tableInformation.setName(tableAnnotation.name());
            tableInformation.setPrimaryKeys(tableAnnotation.primaryKeys());
            tableInformation.setUniqueConstraint(tableAnnotation.uniqueConstraint());
        }
        return tableInformation;
    }

    public static String getTableName(Class<?> pTableClass) {
        String tableName = null;
        if (pTableClass.isAnnotationPresent(Table.class)) {
            Table tableAnnotation = pTableClass.getAnnotation(Table.class);
            tableName = tableAnnotation.name();
            if (TextUtils.isEmpty(tableName)) {
                tableName = pTableClass.getSimpleName();
            }
        }
        return tableName;
    }

    public static String[] getPrimaryKeys(Class<?> pTableClass) {
        String[] primaryKeys = null;
        if (pTableClass.isAnnotationPresent(Table.class)) {
            Table tableAnnotation = pTableClass.getAnnotation(Table.class);
            primaryKeys = tableAnnotation.primaryKeys();
        }
        return primaryKeys;
    }

    public static String[] getUniqueConstraint(Class<?> pTableClass) {
        String[] uniqueConstraint = null;
        if (pTableClass.isAnnotationPresent(Table.class)) {
            Table tableAnnotation = pTableClass.getAnnotation(Table.class);
            uniqueConstraint = tableAnnotation.uniqueConstraint();
        }
        return uniqueConstraint;
    }

    public static ColumnInformation getColumnInformation(Field pColumnField) {
        ColumnInformation columnInformation = null;
        if (pColumnField.isAnnotationPresent(Column.class)) {
            Column columnAnnotation = pColumnField.getAnnotation(Column.class);
            columnInformation = new ColumnInformation();
            columnInformation.setName(columnAnnotation.name());
            columnInformation.setDataType(UtilsDataType.inferColumnDataType(pColumnField));
            columnInformation.setDefaultValue(columnAnnotation.defaultValue());
            columnInformation.setNullable(columnAnnotation.nullable());
            columnInformation.setUnique(columnAnnotation.unique());
            if (UtilsReflection.isPrimaryField(pColumnField)) {
                PrimaryKey primaryKeyAnnotation = pColumnField.getAnnotation(PrimaryKey.class);
                columnInformation.setPrimaryKey(true);
                columnInformation.setAutoIncrement(primaryKeyAnnotation.autoIncrement());
            }
        }
        return columnInformation;
    }

    public static String getColumnName(Field pColumnField) {
        String columnName = null;
        if (pColumnField.isAnnotationPresent(Column.class)) {
            Column columnAnnotation = pColumnField.getAnnotation(Column.class);
            columnName = columnAnnotation.name();
            if (TextUtils.isEmpty(columnName)) {
                columnName = pColumnField.getName();
            }
        }
        return columnName;
    }

    public static boolean isNullable(Field pColumnField) {
        boolean isNullable = false;
        if (pColumnField.isAnnotationPresent(Column.class)) {
            Column columnAnnotation = pColumnField.getAnnotation(Column.class);
            isNullable = columnAnnotation.nullable();
        }
        return isNullable;
    }

    public static boolean isUnique(Field pColumnField) {
        boolean isUnique = false;
        if (pColumnField.isAnnotationPresent(Column.class)) {
            Column columnAnnotation = pColumnField.getAnnotation(Column.class);
            isUnique = columnAnnotation.unique();
        }
        return isUnique;
    }

    public static String getDefaultValue(Field pColumnField) {
        String defaultValue = null;
        if (pColumnField.isAnnotationPresent(Column.class)) {
            Column columnAnnotation = pColumnField.getAnnotation(Column.class);
            defaultValue = columnAnnotation.defaultValue();
        }
        return defaultValue;
    }

    public static boolean isAutoIncrement(Field pColumnField) {
        boolean isAutoIncrement = false;
        String columnType = UtilsDataType.inferColumnDataType(pColumnField);
        if (UtilsDataType.DATA_TYPE__INTEGER.equals(columnType) && UtilsReflection.isPrimaryField(pColumnField)) {
            PrimaryKey primaryKeyAnnotation = pColumnField.getAnnotation(PrimaryKey.class);
            isAutoIncrement = primaryKeyAnnotation.autoIncrement();
        }
        return isAutoIncrement;
    }

    public static boolean isTableClass(Class<?> pClass) {
        boolean isTableClass = false;
        if (pClass.isAnnotationPresent(Table.class)) {
            isTableClass = true;
        }
        return isTableClass;
    }

    public static boolean isColumnField(Field pField) {
        boolean isColumnField = false;
        int modifiers = pField.getModifiers();
        if (!Modifier.isStatic(modifiers)
                && !Modifier.isTransient(modifiers)
                && pField.isAnnotationPresent(Column.class)) {
            isColumnField = true;
        }
        return isColumnField;
    }

    public static boolean isPrimaryField(Field pField) {
        boolean isPrimaryField = false;
        if (UtilsReflection.isColumnField(pField) && pField.isAnnotationPresent(PrimaryKey.class)) {
            isPrimaryField = true;
        }
        return isPrimaryField;
    }

    public static boolean isForeignField(Field pField) {
        boolean isForeignField = false;
        if (UtilsReflection.isColumnField(pField) && pField.isAnnotationPresent(ForeignKey.class)) {
            isForeignField = true;
        }
        return isForeignField;
    }

    public static boolean isSelectableColumnField(Field pField) {
        boolean isSelectableColumnField = false;
        int modifiers = pField.getModifiers();
        if (!Modifier.isStatic(modifiers)
                && !Modifier.isTransient(modifiers)
                && (pField.isAnnotationPresent(Column.class)
                || pField.isAnnotationPresent(Relationship.class))) {
            isSelectableColumnField = true;
        }
        return isSelectableColumnField;
    }

    public static Object[] unpackArray(Object pArrayObject) {
        Object[] returnArray = new Object[Array.getLength(pArrayObject)];
        for(int i = 0; i < returnArray.length; i++) {
            returnArray[i] = Array.get(pArrayObject, i);
        }
        return returnArray;
    }

    public static class TableInformation {
        private String mName;
        private String[] mPrimaryKeys;
        private String[] mUniqueConstraint;

        public String getName() {
            return this.mName;
        }

        public void setName(String pName) {
            this.mName = pName;
        }

        public String[] getPrimaryKeys() {
            return this.mPrimaryKeys;
        }

        public void setPrimaryKeys(String[] pPrimaryKeys) {
            this.mPrimaryKeys = pPrimaryKeys;
        }

        public String[] getUniqueConstraint() {
            return this.mUniqueConstraint;
        }

        public void setUniqueConstraint(String[] pUniqueConstraint) {
            this.mUniqueConstraint = pUniqueConstraint;
        }
    }

    public static class ColumnInformation {
        private String mName;
        private String mDataType;
        private boolean mIsPrimaryKey;
        private boolean mIsAutoIncrement;
        private String mDefaultValue;
        private boolean mIsNullable;
        private boolean mIsUnique;

        public String getName() {
            return this.mName;
        }

        public void setName(String pName) {
            this.mName = pName;
        }

        public String getDataType() {
            return this.mDataType;
        }

        public void setDataType(String pDataType) {
            this.mDataType = pDataType;
        }

        public boolean isPrimaryKey() {
            return this.mIsPrimaryKey;
        }

        public void setPrimaryKey(boolean pIsPrimaryKey) {
            this.mIsPrimaryKey = pIsPrimaryKey;
        }

        public boolean isAutoIncrement() {
            return this.mIsAutoIncrement;
        }

        public void setAutoIncrement(boolean pIsAutoIncrement) {
            this.mIsAutoIncrement = pIsAutoIncrement;
        }

        public String getDefaultValue() {
            return this.mDefaultValue;
        }

        public void setDefaultValue(String pDefaultValue) {
            this.mDefaultValue = pDefaultValue;
        }

        public boolean isNullable() {
            return this.mIsNullable;
        }

        public void setNullable(boolean pIsNullable) {
            this.mIsNullable = pIsNullable;
        }

        public boolean isUnique() {
            return this.mIsUnique;
        }

        public void setUnique(boolean pIsUnique) {
            this.mIsUnique = pIsUnique;
        }
    }
}
