package com.dreamdigitizers.androidsqliteorm.utilities;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class UtilsDatabaseSchema {
    private static final String TAG = "UtilsDatabaseSchema";

    public static void createDatabaseSchema(Context pContext, SQLiteDatabase pSQLiteDatabase) {
        try {
            List<Class<?>> tableClasses = UtilsReflection.getTableClasses(pContext);
            for (Class<?> tableClass : tableClasses) {
                UtilsDatabaseSchema.createTable(tableClass, pSQLiteDatabase);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateDatabaseSchema(Context pContext, SQLiteDatabase pSQLiteDatabase) {
        try {
            List<Class<?>> tableClasses = UtilsReflection.getTableClasses(pContext);
            for (Class<?> tableClass : tableClasses) {
                UtilsDatabaseSchema.createTable(tableClass, pSQLiteDatabase);
                String tableName = UtilsReflection.getTableName(tableClass);
                Cursor cursor = pSQLiteDatabase.query("sqlite_master", null, "type = 'table' AND name = ?", new String[] { tableName }, null, null, null);
                if (cursor.getCount() == 0) {
                    UtilsDatabaseSchema.createTable(tableClass, pSQLiteDatabase);
                } else {
                    UtilsDatabaseSchema.updateTableIfNecessary(tableClass, pSQLiteDatabase);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createTable(Class<?> pTableClass, SQLiteDatabase pSQLiteDatabase) {
        String sql = UtilsDatabaseSchema.createTableSQL(pTableClass);
        Log.d(UtilsDatabaseSchema.TAG, sql);
        if (!TextUtils.isEmpty(sql)) {
            pSQLiteDatabase.execSQL(sql);
        }
    }

    public static void updateTableIfNecessary(Class<?> pTableClass, SQLiteDatabase pSQLiteDatabase) {
        List<Field> columnFields = UtilsReflection.getAllColumnFields(pTableClass);
        String tableName = UtilsReflection.getTableName(pTableClass);
        List<String> presentColumns = UtilsDatabaseSchema.getAllColumnNames(tableName, pSQLiteDatabase);

        for (Field columnField : columnFields) {
            UtilsReflection.ColumnInformation columnInformation = UtilsReflection.getColumnInformation(columnField);
            String columnName = columnInformation.getName();
            String dataType = columnInformation.getDataType();
            String defaultValue = columnInformation.getDefaultValue();
            boolean isNullable = columnInformation.isNullable();
            boolean isUnique = columnInformation.isUnique();

            if (!presentColumns.contains(columnName)) {
                StringBuilder stringBuilder = new StringBuilder("ALTER TABLE ");
                stringBuilder.append(tableName);
                stringBuilder.append(" ADD COLUMN ");
                stringBuilder.append(columnName);
                stringBuilder.append(" ");
                stringBuilder.append(dataType);

                if (!TextUtils.isEmpty(defaultValue)) {
                    stringBuilder.append(" DEFAULT '");
                    stringBuilder.append(defaultValue);
                    stringBuilder.append("'");
                }

                if (!isNullable) {
                    stringBuilder.append(" NOT NULL");
                }

                if (isUnique) {
                    stringBuilder.append(" UNIQUE");
                }

                String sql = stringBuilder.toString();
                Log.d(UtilsDatabaseSchema.TAG, sql);
                pSQLiteDatabase.execSQL(sql);
            }
        }
    }

    public static String createTableSQL(Class pTableClass) {
        UtilsReflection.TableInformation tableInformation = UtilsReflection.getTableInformation(pTableClass);
        String tableName = tableInformation.getName();
        String[] primaryKeys = tableInformation.getPrimaryKeys();
        String[] uniqueConstraint = tableInformation.getUniqueConstraint();

        StringBuilder stringBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        stringBuilder.append(tableName);
        stringBuilder.append("(");

        int i = 0;
        List<Field> columnFields = UtilsReflection.getAllColumnFields(pTableClass);
        for (Field columnField : columnFields) {
            UtilsReflection.ColumnInformation columnInformation = UtilsReflection.getColumnInformation(columnField);
            String columnName = columnInformation.getName();
            String dataType = columnInformation.getDataType();
            boolean isPrimaryKey = columnInformation.isPrimaryKey();
            boolean isAutoIncrement = columnInformation.isAutoIncrement();
            String defaultValue = columnInformation.getDefaultValue();
            boolean isNullable = columnInformation.isNullable();
            boolean isUnique = columnInformation.isUnique();

            stringBuilder.append(columnName);
            stringBuilder.append(" ");
            stringBuilder.append(dataType);

            if ((primaryKeys == null || primaryKeys.length == 0) && isPrimaryKey) {
                stringBuilder.append(" PRIMARY KEY");

                if (isAutoIncrement) {
                    stringBuilder.append(" AUTOINCREMENT");
                }
            }

            if (!TextUtils.isEmpty(defaultValue)) {
                stringBuilder.append(" DEFAULT '");
                stringBuilder.append(defaultValue);
                stringBuilder.append("'");
            }

            if (!isNullable) {
                stringBuilder.append(" NOT NULL");
            }

            if (isUnique) {
                stringBuilder.append(" UNIQUE");
            }

            if (i < columnFields.size() - 1) {
                stringBuilder.append(", ");
            }

            i++;
        }

        if (primaryKeys != null && primaryKeys.length > 0) {
            stringBuilder.append(", PRIMARY KEY(");

            int j = 0;
            for(String columnName : primaryKeys) {
                stringBuilder.append(columnName);

                if (j < (primaryKeys.length - 1)) {
                    stringBuilder.append(", ");
                }

                j++;
            }

            stringBuilder.append(")");
        }

        if (uniqueConstraint != null && uniqueConstraint.length > 0) {
            stringBuilder.append(", UNIQUE(");

            int j = 0;
            for(String columnName : uniqueConstraint) {
                stringBuilder.append(columnName);

                if (j < (uniqueConstraint.length - 1)) {
                    stringBuilder.append(", ");
                }

                j++;
            }

            stringBuilder.append(")");
        }

        stringBuilder.append(")");

        String sql = stringBuilder.toString();
        return sql;
    }

    public static List<String> getAllColumnNames(String pTableName, SQLiteDatabase pSQLiteDatabase) {
        Cursor cursor = pSQLiteDatabase.query(pTableName, null, null, null, null, null, null);
        List<String> columnNames = new ArrayList<>();
        int columnCount = cursor.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            String columnName = cursor.getColumnName(i);
            columnNames.add(columnName);
        }
        cursor.close();
        return columnNames;
    }
}
