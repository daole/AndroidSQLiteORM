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
            String columnDataType = columnInformation.getDataType();
            String columnDefaultValue = columnInformation.getDefaultValue();
            boolean isColumnNullable = columnInformation.isNullable();
            boolean isColumnUnique = columnInformation.isUnique();

            if (!presentColumns.contains(columnName)) {
                StringBuilder stringBuilder = new StringBuilder("ALTER TABLE ");
                stringBuilder.append(tableName);
                stringBuilder.append(" ADD COLUMN ");
                stringBuilder.append(columnName);
                stringBuilder.append(" ");
                stringBuilder.append(columnDataType);

                if (!TextUtils.isEmpty(columnDefaultValue)) {
                    stringBuilder.append(" DEFAULT '");
                    stringBuilder.append(columnDefaultValue);
                    stringBuilder.append("'");
                }

                if (!isColumnNullable) {
                    stringBuilder.append(" NOT NULL");
                }

                if (isColumnUnique) {
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
        String[] tablePrimaryKeys = tableInformation.getPrimaryKeys();
        String[] tableUniqueConstraint = tableInformation.getUniqueConstraint();

        StringBuilder stringBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        stringBuilder.append(tableName);
        stringBuilder.append("(");

        int i = 0;
        List<Field> columnFields = UtilsReflection.getAllColumnFields(pTableClass);
        for (Field columnField : columnFields) {
            UtilsReflection.ColumnInformation columnInformation = UtilsReflection.getColumnInformation(columnField);
            String columnName = columnInformation.getName();
            String columnDataType = columnInformation.getDataType();
            boolean isColumnPrimaryKey = columnInformation.isPrimaryKey();
            boolean isColumnAutoIncrement = columnInformation.isAutoIncrement();
            String columnDefaultValue = columnInformation.getDefaultValue();
            boolean isColumnNullable = columnInformation.isNullable();
            boolean isColumnUnique = columnInformation.isUnique();

            stringBuilder.append(columnName);
            stringBuilder.append(" ");
            stringBuilder.append(columnDataType);

            if ((tablePrimaryKeys == null || tablePrimaryKeys.length == 0) && isColumnPrimaryKey) {
                stringBuilder.append(" PRIMARY KEY");

                if (isColumnAutoIncrement) {
                    stringBuilder.append(" AUTOINCREMENT");
                }
            }

            if (!TextUtils.isEmpty(columnDefaultValue)) {
                stringBuilder.append(" DEFAULT '");
                stringBuilder.append(columnDefaultValue);
                stringBuilder.append("'");
            }

            if (!isColumnNullable) {
                stringBuilder.append(" NOT NULL");
            }

            if (isColumnUnique) {
                stringBuilder.append(" UNIQUE");
            }

            if (i < columnFields.size() - 1) {
                stringBuilder.append(", ");
            }

            i++;
        }

        if (tablePrimaryKeys != null && tablePrimaryKeys.length > 0) {
            stringBuilder.append(", PRIMARY KEY(");

            int j = 0;
            for(String columnName : tablePrimaryKeys) {
                stringBuilder.append(columnName);

                if (j < (tablePrimaryKeys.length - 1)) {
                    stringBuilder.append(", ");
                }

                j++;
            }

            stringBuilder.append(")");
        }

        if (tableUniqueConstraint != null && tableUniqueConstraint.length > 0) {
            stringBuilder.append(", UNIQUE(");

            int j = 0;
            for(String columnName : tableUniqueConstraint) {
                stringBuilder.append(columnName);

                if (j < (tableUniqueConstraint.length - 1)) {
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
