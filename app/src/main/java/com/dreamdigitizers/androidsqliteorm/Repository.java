package com.dreamdigitizers.androidsqliteorm;

import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;

import com.dreamdigitizers.androidsqliteorm.helpers.HelperSQLite;
import com.dreamdigitizers.androidsqliteorm.helpers.HelperSQLiteConfiguration;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsDataType;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsNaming;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsQuery;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsReflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Repository {
    private static Repository instance;

    private HelperSQLite mHelperSQLite;
    private SQLiteQueryBuilder mSQLiteQueryBuilder;

    public static void initialize(HelperSQLiteConfiguration pHelperSQLiteConfiguration) {
        Repository.instance = new Repository(pHelperSQLiteConfiguration);
    }

    public static Repository getInstance() {
        if (instance == null) {
            throw new RuntimeException("Repository has not been yet initialized. Please call Repository.initialize(pSQLiteConfigurationInformation) first.");
        }
        return Repository.instance;
    }

    private Repository(HelperSQLiteConfiguration pHelperSQLiteConfiguration) {
        this.mHelperSQLite = new HelperSQLite(pHelperSQLiteConfiguration);
        this.mSQLiteQueryBuilder = new SQLiteQueryBuilder();
    }

    public <T> List<T> selectAll(Class<T> pTableClass) {
        List<String> pProjection = new ArrayList<>();
        StringBuilder tableClauseBuilder = new StringBuilder();
        UtilsQuery.buildProjectionsAndTableClause(pProjection, tableClauseBuilder, pTableClass);

        String tableClause = tableClauseBuilder.toString();
        this.mSQLiteQueryBuilder.setTables(tableClause);
        Cursor cursor = this.mHelperSQLite.select(this.mSQLiteQueryBuilder, pProjection.toArray(new String[0]), null, null, null, null, null, null);

        List<T> list = this.fetchData(cursor, pTableClass);
        return list;
    }

    public <T> T selectByID(Class<T> pTableClass, Object pID) {
        return null;
    }

    private <T> List<T> fetchData(Cursor pCursor, Class<T> pTableClass) {
        List<T> list = new ArrayList<>();
        if (pCursor.moveToFirst()) {
            try {
                this.fetchData(pCursor, pTableClass, null, new HashMap<Class, Integer>(), null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
        pCursor.close();
        return list;
    }

    private <T> List<T> fetchData(Cursor pCursor, Class<T> pTableClass, String pTableAlias, HashMap<Class, Integer> pTableAliasHashMap, UtilsQuery.Relationship pRelationship) throws IllegalAccessException, InstantiationException {
        String tableName = UtilsReflection.getTableName(pTableClass);
        if (TextUtils.isEmpty(pTableAlias)) {
            pTableAlias = UtilsNaming.buildTableAlias(pTableClass, pTableAliasHashMap);
        }

        List<Field> selectableColumnFields = UtilsReflection.getAllSelectableColumnFields(pTableClass);
        do {
            T instance = pTableClass.newInstance();
            for(Field selectableColumnField : selectableColumnFields) {
                Class<?> columnDataType = UtilsReflection.extractEssentialFieldType(selectableColumnField);

                if (UtilsDataType.isSQLitePrimitiveDataType(columnDataType)) {
                    // Process normal column

                    String columnName = UtilsReflection.getColumnName(selectableColumnField);
                    String columnAlias = UtilsNaming.buildColumnAlias(pTableAlias, columnName);
                    int columnIndex = pCursor.getColumnIndex(columnAlias);
                    Object value = this.getColumnValue(pCursor, columnIndex, columnDataType);
                    selectableColumnField.setAccessible(true);
                    selectableColumnField.set(instance, value);
                } else {
                    // Process column referencing another table
                }
            }
        } while (pCursor.moveToNext());

        return null;
    }

    private Object getColumnValue(Cursor pCursor, int pColumnIndex, Class<?> pClass) {
        Object object = null;
        if (Byte.TYPE.isAssignableFrom(pClass) || Byte.class.isAssignableFrom(pClass)) {
            object = pCursor.getShort(pColumnIndex);
        }

        if (Short.TYPE.isAssignableFrom(pClass) || Short.class.isAssignableFrom(pClass)) {
            object = pCursor.getShort(pColumnIndex);
        }

        if (Integer.TYPE.isAssignableFrom(pClass) || Integer.class.isAssignableFrom(pClass)) {
            object = pCursor.getInt(pColumnIndex);
        }

        if (Integer.TYPE.isAssignableFrom(pClass) || Integer.class.isAssignableFrom(pClass)) {
            object = pCursor.getInt(pColumnIndex);
        }

        if (Long.TYPE.isAssignableFrom(pClass) || Long.class.isAssignableFrom(pClass)) {
            object = pCursor.getLong(pColumnIndex);
        }

        if (Float.TYPE.isAssignableFrom(pClass) || Float.class.isAssignableFrom(pClass)) {
            object = pCursor.getFloat(pColumnIndex);
        }

        if (Double.TYPE.isAssignableFrom(pClass) || Double.class.isAssignableFrom(pClass)) {
            object = pCursor.getDouble(pColumnIndex);
        }

        if (Boolean.TYPE.isAssignableFrom(pClass) || Boolean.class.isAssignableFrom(pClass)) {
            int value = pCursor.getInt(pColumnIndex);
            if (value > 0) {
                object = true;
            } else {
                object = false;
            }
        }

        if (java.util.Date.class.isAssignableFrom(pClass) || java.sql.Date.class.isAssignableFrom(pClass)) {
            long value = pCursor.getLong(pColumnIndex);
            if (java.util.Date.class.isAssignableFrom(pClass)) {
                object = new java.util.Date(value);
            } else if (java.sql.Date.class.isAssignableFrom(pClass)) {
                object = new java.sql.Date(value);
            }
        }

        if (UtilsDataType.isSQLiteTextDataType(pClass)) {
            String value = pCursor.getString(pColumnIndex);
            if (Character.TYPE.isAssignableFrom(pClass) || Character.class.isAssignableFrom(pClass)) {
                object = value.charAt(0);
            } else {
                object = value;
            }
        }

        if (UtilsDataType.isSQLiteBlobDataType(pClass)) {
            object = pCursor.getBlob(pColumnIndex);
        }

        return object;
    }
}
