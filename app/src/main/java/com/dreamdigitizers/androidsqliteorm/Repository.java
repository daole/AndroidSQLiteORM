package com.dreamdigitizers.androidsqliteorm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;

import com.dreamdigitizers.androidsqliteorm.annotations.ForeignKey;
import com.dreamdigitizers.androidsqliteorm.annotations.ManyToOne;
import com.dreamdigitizers.androidsqliteorm.annotations.OneToMany;
import com.dreamdigitizers.androidsqliteorm.annotations.OneToOne;
import com.dreamdigitizers.androidsqliteorm.helpers.HelperSQLite;
import com.dreamdigitizers.androidsqliteorm.helpers.HelperSQLiteConfiguration;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsDataType;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsNaming;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsQuery;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsReflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

        //tableClause = "product_order";
        //pProjection.clear();

        this.mSQLiteQueryBuilder.setTables(tableClause);
        Cursor cursor = this.mHelperSQLite.select(this.mSQLiteQueryBuilder, pProjection.toArray(new String[0]), null, null, null, null, null, null);

        List<T> list = new ArrayList<>();
        try {
            list = this.fetchData(cursor, pTableClass);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return list;
    }

    public <T> T selectByID(Class<T> pTableClass, Object pID) {
        return null;
    }

    public void insert(Object pEntity) {
        try {
            String tableName = UtilsReflection.getTableName(pEntity.getClass());
            ContentValues contentValues = this.buildContentValues(pEntity);
            this.mHelperSQLite.insert(tableName, contentValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private <T> List<T> fetchData(Cursor pCursor, Class<T> pTableClass) throws InstantiationException, IllegalAccessException {
        List<T> list = new ArrayList<>();
        if (pCursor.moveToFirst()) {
            HashMap<String, HashMap<Object, Object>> processedRecordsHashMap = new HashMap<>();
            do {
                T entity = this.fetchDataAtCurrentRow(pCursor, pTableClass, null, new HashMap<Class, Integer>(), processedRecordsHashMap, null);
                if (!list.contains(entity)) {
                    list.add(entity);
                }
            } while (pCursor.moveToNext());
        }
        pCursor.close();
        return list;
    }

    private <T> T fetchDataAtCurrentRow(Cursor pCursor, Class<T> pTableClass, String pTableAlias, HashMap<Class, Integer> pTableAliasesHashMap, HashMap<String, HashMap<Object, Object>> pProcessedRecordsHashMap, Relationship pRelationship) throws IllegalAccessException, InstantiationException {
        if (TextUtils.isEmpty(pTableAlias)) {
            pTableAlias = UtilsNaming.buildTableAlias(pTableClass, pTableAliasesHashMap);
        }

        Object primaryKeyValue = this.getPrimaryColumnValue(pCursor, pTableClass, pTableAlias);
        if (primaryKeyValue == null) {
            return null;
        }

        HashMap<Object, Object> processedRecords;
        if (pProcessedRecordsHashMap.containsKey(pTableAlias)) {
            processedRecords = pProcessedRecordsHashMap.get(pTableAlias);
        } else {
            processedRecords = new HashMap<>();
            pProcessedRecordsHashMap.put(pTableAlias, processedRecords);
        }

        // Only create new record only if this record has not been fetched
        T record;
        if (processedRecords.containsKey(primaryKeyValue)) {
            record = (T) processedRecords.get(primaryKeyValue);
        } else {
            record = pTableClass.newInstance();
            processedRecords.put(primaryKeyValue, record);
        }

        Object relationshipValue;

        List<Field> selectableColumnFields = UtilsReflection.getAllSelectableColumnFields(pTableClass);

        for (Field selectableColumnField : selectableColumnFields) {
            selectableColumnField.setAccessible(true);
            Class<?> columnDataType = UtilsReflection.extractEssentialFieldType(selectableColumnField);

            if (UtilsDataType.isSQLitePrimitiveDataType(columnDataType)) {
                // Process normal column

                Object columnValue = this.getColumnValue(pCursor, selectableColumnField, columnDataType, pTableAlias);
                selectableColumnField.set(record, columnValue);
            } else {
                // Process column referencing another table

                // boolean annotationOptional;
                Class<?> annotationForeignTableClass;
                String annotationForeignColumnName;
                FetchType annotationFetchType;

                // Retrieve relationship information
                if (selectableColumnField.isAnnotationPresent(OneToOne.class)) {
                    OneToOne oneToOneAnnotation = selectableColumnField.getAnnotation(OneToOne.class);

                    // annotationOptional = oneToOneAnnotation.optional();
                    annotationForeignTableClass = oneToOneAnnotation.foreignTableClass();
                    annotationForeignColumnName = oneToOneAnnotation.foreignColumnName();
                    annotationFetchType = oneToOneAnnotation.fetchType();
                } else if (selectableColumnField.isAnnotationPresent(OneToMany.class)) {
                    OneToMany oneToManyAnnotation = selectableColumnField.getAnnotation(OneToMany.class);

                    // annotationOptional = oneToManyAnnotation.optional();
                    annotationForeignTableClass = oneToManyAnnotation.foreignTableClass();
                    annotationForeignColumnName = oneToManyAnnotation.foreignColumnName();
                    annotationFetchType = oneToManyAnnotation.fetchType();
                } else {
                    ManyToOne manyToOneAnnotation = selectableColumnField.getAnnotation(ManyToOne.class);

                    // annotationOptional = manyToOneAnnotation.optional();
                    annotationForeignTableClass = manyToOneAnnotation.foreignTableClass();
                    annotationForeignColumnName = manyToOneAnnotation.foreignColumnName();
                    annotationFetchType = manyToOneAnnotation.fetchType();
                }

                if (annotationFetchType == FetchType.EAGER) {
                    boolean isForeignField = UtilsReflection.isForeignField(selectableColumnField);

                    Class<?> nextProcessedTableClass;

                    Class<?> primaryTableClass;
                    String primaryTableAlias = null;

                    Field primaryColumnField;

                    Class<?> foreignTableClass;
                    String foreignTableAlias = null;

                    Field foreignColumnField;

                    if (isForeignField) {
                        // This column is a foreign key

                        ForeignKey foreignKeyAnnotation = selectableColumnField.getAnnotation(ForeignKey.class);

                        // Foreign table is the table currently being processed
                        foreignTableClass = pTableClass;
                        foreignTableAlias = pTableAlias;

                        // Foreign column this the column currently being processed
                        foreignColumnField = selectableColumnField;

                        // Primary table class is the class of foreign column
                        primaryTableClass = columnDataType;
                        nextProcessedTableClass = primaryTableClass;

                        // Primary column is the primary column specified in foreign key annotation
                        String primaryColumnName = foreignKeyAnnotation.primaryColumnName();
                        if (TextUtils.isEmpty(primaryColumnName)) {
                            primaryColumnName = UtilsReflection.getColumnName(selectableColumnField);
                        }
                        primaryColumnField = UtilsReflection.findColumnFieldByColumnName(primaryColumnName, primaryTableClass);

                        // Retrieve the relationship's value (foreign column's value)
                        Class<?> primaryColumnDataType = UtilsReflection.extractEssentialFieldType(primaryColumnField);
                        relationshipValue = this.getColumnValue(pCursor, foreignColumnField, primaryColumnDataType, pTableAlias);
                    } else {
                        // Process foreign table and foreign column if they are specified in the relationship annotation

                        // Check if foreign table is specified
                        if (annotationForeignTableClass == void.class) {
                            annotationForeignTableClass = columnDataType;
                        }

                        // Foreign table is the table specified in the relationship annotation
                        foreignTableClass = annotationForeignTableClass;
                        nextProcessedTableClass = foreignTableClass;

                        // Check if the foreign column exists in the foreign table
                        // Foreign column is the column specified in the relationship annotation
                        foreignColumnField = UtilsReflection.findColumnFieldByColumnName(annotationForeignColumnName, annotationForeignTableClass);

                        // Primary table class is the class of foreign column
                        primaryTableClass = foreignColumnField.getType();
                        primaryTableAlias = pTableAlias;

                        ForeignKey foreignKeyAnnotation = foreignColumnField.getAnnotation(ForeignKey.class);

                        // Primary column is the primary column specified in foreign key annotation
                        String primaryColumnName = foreignKeyAnnotation.primaryColumnName();
                        if (TextUtils.isEmpty(primaryColumnName)) {
                            primaryColumnName = UtilsReflection.getColumnName(foreignColumnField);
                        }
                        primaryColumnField = UtilsReflection.findColumnFieldByColumnName(primaryColumnName, primaryTableClass);

                        // Retrieve the relationship's value (primary column's value)
                        Class<?> primaryColumnDataType = UtilsReflection.extractEssentialFieldType(primaryColumnField);
                        relationshipValue = this.getColumnValue(pCursor, primaryColumnField, primaryColumnDataType, pTableAlias);
                    }

                    Object columnValue;

                    // Check if this join is already processed
                    Relationship relationship = new Relationship(primaryTableClass, primaryColumnField, foreignTableClass, foreignColumnField, record, relationshipValue);
                    if (!relationship.equals(pRelationship)) {
                        if (TextUtils.isEmpty(primaryTableAlias)) {
                            primaryTableAlias = UtilsNaming.buildTableAlias(primaryTableClass, pTableAliasesHashMap);
                        }

                        if (TextUtils.isEmpty(foreignTableAlias)) {
                            foreignTableAlias = UtilsNaming.buildTableAlias(foreignTableClass, pTableAliasesHashMap);
                        }

                        String nextProcessedTableAlias = null;
                        if (nextProcessedTableClass.equals(primaryTableClass)) {
                            nextProcessedTableAlias = primaryTableAlias;
                        } else if (nextProcessedTableClass.equals(foreignTableClass)) {
                            nextProcessedTableAlias = foreignTableAlias;
                        }

                        columnValue = this.fetchDataAtCurrentRow(pCursor, nextProcessedTableClass, nextProcessedTableAlias, pTableAliasesHashMap, pProcessedRecordsHashMap, relationship);
                    } else {
                        columnValue = pRelationship.mRelationshipEntity;
                    }

                    if (columnValue != null) {
                        Object selectableColumnFieldValue = selectableColumnField.get(record);
                        Class<?> selectableColumnFieldType = selectableColumnField.getType();
                        if (selectableColumnFieldType.isArray()) {
                            List<Object> listValues;
                            if (selectableColumnFieldValue == null) {
                                listValues = new ArrayList<>();
                            } else {
                                Object[] arrayValues = UtilsReflection.unpackArray(selectableColumnFieldValue);
                                listValues = Arrays.asList(arrayValues);
                            }
                            if (!listValues.contains(columnValue)) {
                                listValues.add(columnValue);
                            }
                            Object[] arrayValues = listValues.toArray();
                            selectableColumnField.set(record, arrayValues);
                        } else if (Collection.class.isAssignableFrom(selectableColumnFieldType)) {
                            Collection collectionValues = (Collection) selectableColumnFieldValue;
                            if (collectionValues == null) {
                                collectionValues = new ArrayList();
                                selectableColumnField.set(record, collectionValues);
                            }
                            if (!collectionValues.contains(columnValue)) {
                                collectionValues.add(columnValue);
                            }
                        } else {
                            selectableColumnField.set(record, columnValue);
                        }
                    }
                }
            }
        }

        return record;
    }

    private ContentValues buildContentValues(Object pEntity) throws IllegalAccessException {
        ContentValues contentValues = new ContentValues();
        Class<?> tableClass = pEntity.getClass();
        if (UtilsReflection.isTableClass(tableClass)) {
            List<Field> columnFields = UtilsReflection.getAllColumnFields(tableClass);
            for (Field columnField : columnFields) {
                this.setColumnValue(pEntity, columnField, contentValues);
            }
        }
        return contentValues;
    }

    private Object getPrimaryColumnValue(Cursor pCursor, Class<?> pTableClass, String pTableAlias) {
        Field primaryColumnField = UtilsReflection.findPrimaryColumnField(pTableClass);
        Class<?> primaryColumnDataType = UtilsReflection.extractEssentialFieldType(primaryColumnField);
        Object columnValue = this.getColumnValue(pCursor, primaryColumnField, primaryColumnDataType, pTableAlias);
        return columnValue;
    }

    private Object getColumnValue(Cursor pCursor, Field pColumnField, Class<?> pColumnDataType, String pTableAlias) {
        String columnName = UtilsReflection.getColumnName(pColumnField);
        String columnAlias = UtilsNaming.buildColumnAlias(pTableAlias, columnName, false);
        int columnIndex = pCursor.getColumnIndex(columnAlias);
        Object columnValue = this.getColumnValue(pCursor, columnIndex, pColumnDataType);
        return columnValue;
    }

    private Object getColumnValue(Cursor pCursor, int pColumnIndex, Class<?> pColumnDataType) {
        Object columnValue = null;

        if (!pCursor.isNull(pColumnIndex)) {
            if (Byte.TYPE.isAssignableFrom(pColumnDataType) || Byte.class.isAssignableFrom(pColumnDataType)) {
                columnValue = pCursor.getShort(pColumnIndex);
            } else if (Short.TYPE.isAssignableFrom(pColumnDataType) || Short.class.isAssignableFrom(pColumnDataType)) {
                columnValue = pCursor.getShort(pColumnIndex);
            } else if (Integer.TYPE.isAssignableFrom(pColumnDataType) || Integer.class.isAssignableFrom(pColumnDataType)) {
                columnValue = pCursor.getInt(pColumnIndex);
            } else if (Long.TYPE.isAssignableFrom(pColumnDataType) || Long.class.isAssignableFrom(pColumnDataType)) {
                columnValue = pCursor.getLong(pColumnIndex);
            } else if (Float.TYPE.isAssignableFrom(pColumnDataType) || Float.class.isAssignableFrom(pColumnDataType)) {
                columnValue = pCursor.getFloat(pColumnIndex);
            } else if (Double.TYPE.isAssignableFrom(pColumnDataType) || Double.class.isAssignableFrom(pColumnDataType)) {
                columnValue = pCursor.getDouble(pColumnIndex);
            } else if (Boolean.TYPE.isAssignableFrom(pColumnDataType) || Boolean.class.isAssignableFrom(pColumnDataType)) {
                int value = pCursor.getInt(pColumnIndex);
                if (value > 0) {
                    columnValue = true;
                } else {
                    columnValue = false;
                }
            } else if (java.util.Date.class.isAssignableFrom(pColumnDataType) || java.sql.Date.class.isAssignableFrom(pColumnDataType)) {
                long value = pCursor.getLong(pColumnIndex);
                if (java.util.Date.class.isAssignableFrom(pColumnDataType)) {
                    columnValue = new java.util.Date(value);
                } else if (java.sql.Date.class.isAssignableFrom(pColumnDataType)) {
                    columnValue = new java.sql.Date(value);
                }
            } else if (UtilsDataType.isSQLiteTextDataType(pColumnDataType)) {
                String value = pCursor.getString(pColumnIndex);
                if (Character.TYPE.isAssignableFrom(pColumnDataType) || Character.class.isAssignableFrom(pColumnDataType)) {
                    columnValue = value.charAt(0);
                } else {
                    columnValue = value;
                }
            } else if (UtilsDataType.isSQLiteBlobDataType(pColumnDataType)) {
                columnValue = pCursor.getBlob(pColumnIndex);
            }
        }

        return columnValue;
    }

    private void setColumnValue(Object pEntity, Field pColumnField, ContentValues pContentValues) throws IllegalAccessException {
        String columnName = UtilsReflection.getColumnName(pColumnField);

        pColumnField.setAccessible(true);
        Object columnValue = pColumnField.get(pEntity);

        if (columnValue == null) {
            pContentValues.putNull(columnName);
        } else {
            Class<?> columnDataType = pColumnField.getType();
            if (UtilsReflection.isForeignField(pColumnField) && UtilsReflection.isTableClass(columnDataType)) {
                ForeignKey foreignKeyAnnotation = pColumnField.getAnnotation(ForeignKey.class);

                String primaryColumnName = foreignKeyAnnotation.primaryColumnName();
                if (TextUtils.isEmpty(primaryColumnName)) {
                    primaryColumnName = UtilsReflection.getColumnName(pColumnField);
                }

                Field primaryColumnField = UtilsReflection.findColumnFieldByColumnName(primaryColumnName, columnDataType);
                if (primaryColumnField == null) {
                    throw new RuntimeException(String.format("There is no primaryColumnName '%s' in the table class '%s'.", primaryColumnName, columnDataType.getSimpleName()));
                }
                primaryColumnField.setAccessible(true);
                columnValue = primaryColumnField.get(columnValue);
                columnDataType = primaryColumnField.getType();
            }

            this.setColumnValue(columnName, columnValue, columnDataType, pContentValues);
        }
    }

    private void setColumnValue(String pColumnName, Object pColumnValue, Class<?> pColumnDataType, ContentValues pContentValues) {
        if (Byte.TYPE.isAssignableFrom(pColumnDataType) || Byte.class.isAssignableFrom(pColumnDataType)) {
            pContentValues.put(pColumnName, (Byte) pColumnValue);
        } else if (Short.TYPE.isAssignableFrom(pColumnDataType) || Short.class.isAssignableFrom(pColumnDataType)) {
            pContentValues.put(pColumnName, (Short) pColumnValue);
        } else if (Integer.TYPE.isAssignableFrom(pColumnDataType) || Integer.class.isAssignableFrom(pColumnDataType)) {
            pContentValues.put(pColumnName, (Integer) pColumnValue);
        } else if (Long.TYPE.isAssignableFrom(pColumnDataType) || Long.class.isAssignableFrom(pColumnDataType)) {
            pContentValues.put(pColumnName, (Long) pColumnValue);
        } else if (Float.TYPE.isAssignableFrom(pColumnDataType) || Float.class.isAssignableFrom(pColumnDataType)) {
            pContentValues.put(pColumnName, (Float) pColumnValue);
        } else if (Double.TYPE.isAssignableFrom(pColumnDataType) || Double.class.isAssignableFrom(pColumnDataType)) {
            pContentValues.put(pColumnName, (Double) pColumnValue);
        } else if (Boolean.TYPE.isAssignableFrom(pColumnDataType) || Boolean.class.isAssignableFrom(pColumnDataType)) {
            pContentValues.put(pColumnName, (Boolean) pColumnValue);
        } else if (java.util.Date.class.isAssignableFrom(pColumnDataType) || java.sql.Date.class.isAssignableFrom(pColumnDataType)) {
            if (java.util.Date.class.isAssignableFrom(pColumnDataType)) {
                pContentValues.put(pColumnName, ((java.util.Date) pColumnValue).getTime());
            } else {
                pContentValues.put(pColumnName, ((java.sql.Date) pColumnValue).getTime());
            }
        } else if (UtilsDataType.isSQLiteTextDataType(pColumnDataType)) {
            if (Character.TYPE.isAssignableFrom(pColumnDataType) || Character.class.isAssignableFrom(pColumnDataType)) {
                pContentValues.put(pColumnName, Character.toString((Character) pColumnValue));
            } else {
                pContentValues.put(pColumnName, (String) pColumnValue);
            }
        } else if (UtilsDataType.isSQLiteBlobDataType(pColumnDataType)) {
            pContentValues.put(pColumnName, (byte[]) pColumnValue);
        }
    }

    private static class Relationship extends UtilsQuery.Relationship {
        private Object mRelationshipEntity;
        private Object mRelationshipValue;

        public Relationship(Class<?> pPrimaryTableClass, Field pPrimaryColumnField, Class<?> pForeignTableClass, Field pForeignColumnField, Object pRelationshipEntity, Object pRelationshipValue) {
            super(pPrimaryTableClass, pPrimaryColumnField, pForeignTableClass, pForeignColumnField);
            this.mRelationshipEntity = pRelationshipEntity;
            this.mRelationshipValue = pRelationshipValue;
        }
    }
}
