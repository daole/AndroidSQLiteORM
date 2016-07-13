package com.dreamdigitizers.androidsqliteorm.utilities;

import android.text.TextUtils;

import com.dreamdigitizers.androidsqliteorm.FetchType;
import com.dreamdigitizers.androidsqliteorm.annotations.ForeignKey;
import com.dreamdigitizers.androidsqliteorm.annotations.ManyToOne;
import com.dreamdigitizers.androidsqliteorm.annotations.OneToMany;
import com.dreamdigitizers.androidsqliteorm.annotations.OneToOne;
import com.dreamdigitizers.androidsqliteorm.annotations.Table;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

            List<Field> selectableColumnFields = UtilsReflection.getAllSelectableColumnFields(pTableClass);
            for (Field selectableColumnField : selectableColumnFields) {
                Class<?> columnDataType = UtilsReflection.extractEssentialFieldType(selectableColumnField);

                if (UtilsDataType.isSQLitePrimitiveDataType(columnDataType)) {
                    // Process normal column
                    String columnName = UtilsReflection.getColumnName(selectableColumnField);
                    String columnAlias = UtilsNaming.buildColumnAlias(pTableAlias, columnName);
                    pProjections.add(columnAlias);
                } else {
                    // Process column referencing another table

                    // Check if this column references to a valid table
                    if (!UtilsReflection.isTableClass(columnDataType)) {
                        throw new RuntimeException(String.format("%s is not annotated with %s.", columnDataType.getSimpleName(), Table.class.getSimpleName()));
                    }

                    boolean annotationOptional;
                    Class<?> annotationForeignTableClass;
                    String annotationForeignColumnName;
                    FetchType annotationFetchType;

                    // Retrieve relationship information
                    if (selectableColumnField.isAnnotationPresent(OneToOne.class)) {
                        OneToOne oneToOneAnnotation = selectableColumnField.getAnnotation(OneToOne.class);

                        annotationOptional = oneToOneAnnotation.optional();
                        annotationForeignTableClass = oneToOneAnnotation.foreignTableClass();
                        annotationForeignColumnName = oneToOneAnnotation.foreignColumnName();
                        annotationFetchType = oneToOneAnnotation.fetchType();
                    } else if (selectableColumnField.isAnnotationPresent(OneToMany.class)) {
                        OneToMany oneToManyAnnotation = selectableColumnField.getAnnotation(OneToMany.class);

                        annotationOptional = oneToManyAnnotation.optional();
                        annotationForeignTableClass = oneToManyAnnotation.foreignTableClass();
                        annotationForeignColumnName = oneToManyAnnotation.foreignColumnName();
                        annotationFetchType = oneToManyAnnotation.fetchType();
                    } else if (selectableColumnField.isAnnotationPresent(ManyToOne.class)) {
                        ManyToOne manyToOneAnnotation = selectableColumnField.getAnnotation(ManyToOne.class);

                        annotationOptional = manyToOneAnnotation.optional();
                        annotationForeignTableClass = manyToOneAnnotation.foreignTableClass();
                        annotationForeignColumnName = manyToOneAnnotation.foreignColumnName();
                        annotationFetchType = manyToOneAnnotation.fetchType();
                    } else {
                        throw new RuntimeException(String.format("Field '%s' in the class '%s' must be annotated with one of relationship annotation.", selectableColumnField.getName(), pTableClass.getSimpleName()));
                    }

                    if (annotationFetchType == FetchType.EAGER) {
                        boolean isForeignKey = selectableColumnField.isAnnotationPresent(ForeignKey.class);

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
                            if (isForeignKey) {
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
                                foreignColumnField = UtilsReflection.getColumnFieldByColumnName(annotationForeignColumnName, annotationForeignTableClass);
                                if (foreignColumnField == null) {
                                    throw new RuntimeException(String.format("There is no annotationForeignColumnName field '%s' in the table class '%s'.", annotationForeignColumnName, annotationForeignTableClass.getSimpleName()));
                                }
                                foreignColumnName = annotationForeignColumnName;

                                // Check if the foreign column is a foreign key
                                if (foreignColumnField.isAnnotationPresent(ForeignKey.class)) {
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
                                    primaryColumnField = UtilsReflection.getColumnFieldByColumnName(primaryColumnName, primaryTableClass);
                                    if (primaryColumnField == null) {
                                        throw new RuntimeException(String.format("There is no primaryColumnName '%s' in the table class '%s'.", primaryColumnName, primaryTableClass.getSimpleName()));
                                    }
                                } else {
                                    throw new RuntimeException(String.format("MappedBy field '%s' in the table class '%s' should be annotated with %s.", annotationForeignColumnName, annotationForeignTableClass.getSimpleName(), ForeignKey.class.getSimpleName()));
                                }
                            }
                        } else if (isForeignKey) {
                            // This column is a foreign key

                            ForeignKey foreignKeyAnnotation = selectableColumnField.getAnnotation(ForeignKey.class);

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
                            primaryColumnField = UtilsReflection.getColumnFieldByColumnName(primaryColumnName, primaryTableClass);
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
                }
            }
        } else {
            throw new RuntimeException(String.format("%s is not annotated with %s", pTableClass.getSimpleName(), Table.class.getSimpleName()));
        }
    }

    private static class Relationship {
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
