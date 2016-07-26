package com.dreamdigitizers.androidsqliteorm.utilities;

import android.text.TextUtils;

import com.dreamdigitizers.androidsqliteorm.annotations.ForeignKey;
import com.dreamdigitizers.androidsqliteorm.annotations.Table;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

public class UtilsQuery {
    public static void buildProjectionsAndTableClause(List<String> pProjections, StringBuilder pTableClauseBuilder, Class<?> pTableClass) {
        pProjections.clear();
        pTableClauseBuilder.setLength(0);
        UtilsQuery.buildProjectionsAndTableClause(pProjections, pTableClauseBuilder, pTableClass, null, new HashMap<Class, Integer>(), null);
    }

    public static void buildProjectionsAndTableClause(List<String> pProjections, StringBuilder pTableClauseBuilder, Class<?> pTableClass, String pTableAlias, HashMap<Class, Integer> pTableAliasHashMap, Relationship pRelationship) {
        if (UtilsReflection.isTableClass(pTableClass)) {
            String tableName = UtilsReflection.getTableName(pTableClass);
            if (TextUtils.isEmpty(pTableAlias)) {
                pTableAlias = UtilsNaming.buildTableAlias(pTableClass, pTableAliasHashMap);
            }

            // Add the first processed table class to the table clause
            if (TextUtils.isEmpty(pTableClauseBuilder)) {
                pTableClauseBuilder.append(tableName);
                pTableClauseBuilder.append(" AS ");
                pTableClauseBuilder.append(pTableAlias);
            }

            List<Field> selectableColumnFields = UtilsReflection.getAllSelectableFields(pTableClass);
            for (Field selectableColumnField : selectableColumnFields) {
                Class<?> columnDataType = UtilsReflection.extractEssentialType(selectableColumnField);

                boolean shouldAddToProjections = false;
                if (UtilsDataType.isSQLitePrimitiveDataType(columnDataType)) {
                    // Process normal column

                    shouldAddToProjections = true;
                } else {
                    // Process column referencing another table

                    // Check if this column references to a valid table
                    if (!UtilsReflection.isTableClass(columnDataType)) {
                        throw new RuntimeException(String.format("%s is not annotated with %s.", columnDataType.getSimpleName(), Table.class.getSimpleName()));
                    }

                    boolean annotationOptional;
                    Class<?> annotationForeignTableClass;
                    String annotationForeignColumnName;

                    // Retrieve relationship information
                    if (selectableColumnField.isAnnotationPresent(com.dreamdigitizers.androidsqliteorm.annotations.Relationship.class)) {
                        com.dreamdigitizers.androidsqliteorm.annotations.Relationship relationshipAnnotation = selectableColumnField.getAnnotation(com.dreamdigitizers.androidsqliteorm.annotations.Relationship.class);

                        annotationOptional = relationshipAnnotation.optional();
                        annotationForeignTableClass = relationshipAnnotation.foreignTableClass();
                        annotationForeignColumnName = relationshipAnnotation.foreignColumnName();
                    } else {
                        throw new RuntimeException(String.format("Field '%s' in the class '%s' must be annotated with %s.", selectableColumnField.getName(), pTableClass.getSimpleName(), Relationship.class.getSimpleName()));
                    }

                    boolean isForeignField = UtilsReflection.isForeignColumnField(selectableColumnField);

                    Class<?> nextProcessedTableClass;

                    Class<?> primaryTableClass;
                    String primaryTableAlias = null;

                    Field primaryColumnField;
                    String primaryColumnName;

                    Class<?> foreignTableClass;
                    String foreignTableAlias = null;

                    Field foreignColumnField;
                    String foreignColumnName;

                    // Process foreign table and foreign column if they are specified in the relationship annotation
                    if (annotationForeignTableClass != void.class || !TextUtils.isEmpty(annotationForeignColumnName)) {
                        if (isForeignField) {
                            // If the column being processed is a foreign key, it must not specify foreignColumnName and foreignTableClass
                            throw new RuntimeException(String.format("Field '%s' in the class '%s' is a foreign key, so it must not specify foreignTableClass and foreignColumnName in its relationship annotation.", selectableColumnField.getName(), pTableClass.getSimpleName()));
                        } else {
                            // Check if foreign table is specified
                            if (annotationForeignTableClass == void.class) {
                                annotationForeignTableClass = columnDataType;
                            }

                            // Check if foreign column is specified
                            if (TextUtils.isEmpty(annotationForeignColumnName)) {
                                throw new RuntimeException(String.format("Field '%s' in the class '%s' does not specify annotationForeignColumnName in its relationship annotation", selectableColumnField.getName(), pTableClass.getSimpleName()));
                            }

                            // Foreign table is the table specified in the relationship annotation
                            foreignTableClass = annotationForeignTableClass;
                            nextProcessedTableClass = foreignTableClass;

                            // Check if the foreign column exists in the foreign table
                            // Foreign column is the column specified in the relationship annotation
                            foreignColumnField = UtilsReflection.findColumnFieldByColumnName(annotationForeignColumnName, annotationForeignTableClass);
                            if (foreignColumnField == null) {
                                throw new RuntimeException(String.format("There is no annotationForeignColumnName field '%s' in the table class '%s'.", annotationForeignColumnName, annotationForeignTableClass.getSimpleName()));
                            }
                            foreignColumnName = annotationForeignColumnName;

                            // Check if the foreign column is a foreign key
                            if (UtilsReflection.isForeignColumnField(foreignColumnField)) {
                                ForeignKey foreignKeyAnnotation = foreignColumnField.getAnnotation(ForeignKey.class);

                                // Primary column is the primary column specified in foreign key annotation
                                primaryColumnName = foreignKeyAnnotation.primaryColumnName();
                                if (TextUtils.isEmpty(primaryColumnName)) {
                                    primaryColumnName = UtilsReflection.getColumnName(foreignColumnField);
                                }

                                // Primary table class is the class of foreign column
                                primaryTableClass = foreignColumnField.getType();
                                if (pTableClass != primaryTableClass) {
                                    throw new RuntimeException(String.format("The type of the field '%s' in the table class '%s' should be '%s'.", foreignColumnField.getName(), foreignTableClass.getSimpleName(), pTableClass.getSimpleName()));
                                }
                                primaryTableAlias = pTableAlias;

                                // Check if the primary column exists in the primary table
                                primaryColumnField = UtilsReflection.findColumnFieldByColumnName(primaryColumnName, primaryTableClass);
                                if (primaryColumnField == null) {
                                    throw new RuntimeException(String.format("There is no primaryColumnName '%s' in the table class '%s'.", primaryColumnName, primaryTableClass.getSimpleName()));
                                }
                            } else {
                                throw new RuntimeException(String.format("MappedBy field '%s' in the table class '%s' should be annotated with %s.", annotationForeignColumnName, annotationForeignTableClass.getSimpleName(), ForeignKey.class.getSimpleName()));
                            }
                        }
                    } else if (isForeignField) {
                        // This column is a foreign key

                        ForeignKey foreignKeyAnnotation = selectableColumnField.getAnnotation(ForeignKey.class);

                        shouldAddToProjections = true;

                        // Foreign table is the table currently being processed
                        foreignTableClass = pTableClass;
                        foreignTableAlias = pTableAlias;

                        // Foreign column this the column currently being processed
                        foreignColumnField = selectableColumnField;
                        foreignColumnName = UtilsReflection.getColumnName(selectableColumnField);

                        // Primary table class is the class of foreign column
                        primaryTableClass = columnDataType;
                        nextProcessedTableClass = primaryTableClass;

                        // Primary column is the primary column specified in foreign key annotation
                        primaryColumnName = foreignKeyAnnotation.primaryColumnName();
                        if (TextUtils.isEmpty(primaryColumnName)) {
                            primaryColumnName = foreignColumnName;
                        }

                        // Check if the primary column exists in the primary table
                        primaryColumnField = UtilsReflection.findColumnFieldByColumnName(primaryColumnName, primaryTableClass);
                        if (primaryColumnField == null) {
                            throw new RuntimeException(String.format("There is no column field '%s' in the table class '%s'", primaryColumnName, primaryTableClass.getSimpleName()));
                        }
                    } else {
                        throw new RuntimeException(String.format("Field '%s' in the class '%s' is not a foreign key, so it must specify annotationForeignColumnName in its relationship annotation.", selectableColumnField.getName(), pTableClass.getSimpleName()));
                    }

                    // Check if this join is already processed
                    Relationship relationship = new Relationship(primaryTableClass, primaryColumnField, foreignTableClass, foreignColumnField);
                    if (!relationship.equals(pRelationship)) {
                        if (TextUtils.isEmpty(primaryTableAlias)) {
                            primaryTableAlias = UtilsNaming.buildTableAlias(primaryTableClass, pTableAliasHashMap);
                        }

                        if (TextUtils.isEmpty(foreignTableAlias)) {
                            foreignTableAlias = UtilsNaming.buildTableAlias(foreignTableClass, pTableAliasHashMap);
                        }

                        String nextProcessedTableName = UtilsReflection.getTableName(nextProcessedTableClass);
                        String nextProcessedTableAlias = null;
                        if (nextProcessedTableClass.equals(primaryTableClass)) {
                            nextProcessedTableAlias = primaryTableAlias;
                        } else if (nextProcessedTableClass.equals(foreignTableClass)) {
                            nextProcessedTableAlias = foreignTableAlias;
                        }

                        if (annotationOptional) {
                            pTableClauseBuilder.append(" LEFT");
                        }
                        pTableClauseBuilder.append(" JOIN ");
                        pTableClauseBuilder.append(nextProcessedTableName);
                        pTableClauseBuilder.append(" AS ");
                        pTableClauseBuilder.append(nextProcessedTableAlias);
                        pTableClauseBuilder.append(" ON ");
                        pTableClauseBuilder.append(primaryTableAlias);
                        pTableClauseBuilder.append(".");
                        pTableClauseBuilder.append(primaryColumnName);
                        pTableClauseBuilder.append(" = ");
                        pTableClauseBuilder.append(foreignTableAlias);
                        pTableClauseBuilder.append(".");
                        pTableClauseBuilder.append(foreignColumnName);

                        UtilsQuery.buildProjectionsAndTableClause(pProjections, pTableClauseBuilder, columnDataType, nextProcessedTableAlias, pTableAliasHashMap, relationship);
                    }
                }

                if (shouldAddToProjections) {
                    String columnName = UtilsReflection.getColumnName(selectableColumnField);
                    String columnAlias = UtilsNaming.buildColumnAlias(pTableAlias, columnName);
                    pProjections.add(columnAlias);
                }
            }
        } else {
            throw new RuntimeException(String.format("%s is not annotated with %s", pTableClass.getSimpleName(), Table.class.getSimpleName()));
        }
    }

    public static class Relationship {
        private Class<?> mPrimaryTableClass;
        private Field mPrimaryColumnField;
        private Class<?> mForeignTableClass;
        private Field mForeignColumnField;

        public Relationship(Class<?> pPrimaryTableClass, Field pPrimaryColumnField, Class<?> pForeignTableClass, Field pForeignColumnField) {
            this.mPrimaryTableClass = pPrimaryTableClass;
            this.mPrimaryColumnField = pPrimaryColumnField;
            this.mForeignTableClass = pForeignTableClass;
            this.mForeignColumnField = pForeignColumnField;
        }

        @Override
        public boolean equals(Object pObject) {
            boolean isEqual = false;
            if (pObject != null && pObject instanceof Relationship) {
                Relationship relationship = (Relationship) pObject;
                isEqual = this.mPrimaryTableClass.equals(relationship.mPrimaryTableClass)
                        && this.mPrimaryColumnField.equals(relationship.mPrimaryColumnField)
                        && this.mForeignTableClass.equals(relationship.mForeignTableClass)
                        && this.mForeignColumnField.equals(relationship.mForeignColumnField);
            }
            return isEqual;
        }
    }
}
