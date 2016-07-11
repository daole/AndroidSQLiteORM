package com.dreamdigitizers.androidsqliteorm.utilities;

import android.text.TextUtils;

import com.dreamdigitizers.androidsqliteorm.FetchType;
import com.dreamdigitizers.androidsqliteorm.annotations.ForeignKey;
import com.dreamdigitizers.androidsqliteorm.annotations.ManyToOne;
import com.dreamdigitizers.androidsqliteorm.annotations.OneToMany;
import com.dreamdigitizers.androidsqliteorm.annotations.OneToOne;
import com.dreamdigitizers.androidsqliteorm.annotations.Table;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

public class UtilsQuery {
    public static void buildProjectionsAndTableClause(List<String> pProjections, StringBuilder pTableClauseBuilder, Class<?> pTableClass) {
        pProjections.clear();
        pTableClauseBuilder.setLength(0);
        UtilsQuery.buildProjectionsAndTableClause(pProjections, pTableClauseBuilder, pTableClass, new HashMap<Class, Integer>());
    }

    public static void buildProjectionsAndTableClause(List<String> pProjections, StringBuilder pTableClause, Class<?> pTableClass, HashMap<Class, Integer> pAliasHashMap) {
        if (UtilsReflection.isTableClass(pTableClass)) {
            String tableName = UtilsReflection.getTableName(pTableClass);
            String tableAlias = UtilsNaming.buildTableAlias(pTableClass, pAliasHashMap);

            if (TextUtils.isEmpty(pTableClause)) {
                pTableClause.append(tableName);
                pTableClause.append(" AS ");
                pTableClause.append(tableAlias);
            }

            /*
            boolean isJoin = false;

            if (pTableClause.length() != 0) {
                isJoin = true;

                if (pIsOptional) {
                    pTableClause.append(" LEFT");
                }

                pTableClause.append(" JOIN ");
            }

            String tableAlias = UtilsNaming.buildTableAlias(pTableClass, pAliasHashMap);
            pTableClause.append(tableName);
            pTableClause.append(" AS ");
            pTableClause.append(tableAlias);

            if (isJoin) {
                pTableClause.append(" ON ");
                pTableClause.append(pJoinClause);
            }
            */

            List<Field> selectableColumnFields = UtilsReflection.getAllSelectableColumnFields(pTableClass);
            for (Field selectableColumnField : selectableColumnFields) {
                String columnName = UtilsReflection.getColumnName(selectableColumnField);
                Class<?> columnDataType = selectableColumnField.getType();

                if (columnDataType.isPrimitive()) {
                    // Process normal column
                    String columnAlias = UtilsNaming.buildColumnAlias(tableAlias, columnName);
                    pProjections.add(columnAlias);
                } else {
                    // Process column referencing to another table

                    /*
                    // Check if this column references to a valid table
                    if (!UtilsReflection.isTableClass(columnDataType)) {
                        throw new RuntimeException(String.format("%s is not annotated with %s.", columnDataType.getSimpleName(), Table.class.getSimpleName()));
                    }
                    */

                    boolean optional;
                    Class<?> targetEntity;
                    String mappedBy;
                    FetchType fetchType;

                    // Retrieve relationship information
                    if (selectableColumnField.isAnnotationPresent(OneToOne.class)) {
                        OneToOne oneToOneAnnotation = selectableColumnField.getAnnotation(OneToOne.class);

                        optional = oneToOneAnnotation.optional();
                        targetEntity = oneToOneAnnotation.targetEntity();
                        mappedBy = oneToOneAnnotation.mappedBy();
                        fetchType = oneToOneAnnotation.fetchType();
                    } else if (selectableColumnField.isAnnotationPresent(OneToMany.class)) {
                        OneToMany oneToManyAnnotation = selectableColumnField.getAnnotation(OneToMany.class);

                        optional = oneToManyAnnotation.optional();
                        targetEntity = oneToManyAnnotation.targetEntity();
                        mappedBy = oneToManyAnnotation.mappedBy();
                        fetchType = oneToManyAnnotation.fetchType();
                    } else if (selectableColumnField.isAnnotationPresent(ManyToOne.class)) {
                        ManyToOne manyToOneAnnotation = selectableColumnField.getAnnotation(ManyToOne.class);

                        optional = manyToOneAnnotation.optional();
                        targetEntity = manyToOneAnnotation.targetEntity();
                        mappedBy = manyToOneAnnotation.mappedBy();
                        fetchType = manyToOneAnnotation.fetchType();
                    } else {
                        throw new RuntimeException(String.format("Field '%s' in the class '%s' must be annotated with one of relationship annotation.", selectableColumnField.getName(), pTableClass.getSimpleName()));
                    }

                    if (fetchType == FetchType.EAGER) {
                        boolean isForeignKey = selectableColumnField.isAnnotationPresent(ForeignKey.class);

                        String masterTableName = null;
                        Class<?> masterTableClass = null;

                        String masterColumnName = null;
                        Field masterColumnField = null;

                        String detailTableName = null;
                        Class<?> detailTableClass = null;

                        String detailColumnName = null;
                        Field detailColumnField = null;

                        // Process detail table and detail column if they are specified in the relationship annotation
                        if (targetEntity != void.class || !TextUtils.isEmpty(mappedBy)) {
                            if (isForeignKey) {
                                // If the column being processed is a foreign key, it must not specify detail table and detail column

                                throw new RuntimeException(String.format("Field '%s' in the class '%s' is a foreign key, so it must not specify targetEntity and mappedBy in its relationship annotation.", selectableColumnField.getName(), pTableClass.getSimpleName()));
                            } else {
                                // Check if detail table is specified
                                if (targetEntity == void.class) {
                                    // throw new RuntimeException(String.format("Field '%s' in the class '%s' does not specify targetEntity in its relationship annotation", selectableColumnField.getName(), pTableClass.getSimpleName()));
                                    targetEntity = columnDataType;
                                }

                                // Check if detail column is specified
                                if (TextUtils.isEmpty(mappedBy)) {
                                    throw new RuntimeException(String.format("Field '%s' in the class '%s' does not specify mappedBy in its relationship annotation", selectableColumnField.getName(), pTableClass.getSimpleName()));
                                }

                                // Check if the detail column exists in the detail table
                                Field mappedByField = UtilsReflection.getColumnFieldByColumnName(mappedBy, targetEntity);
                                if (mappedByField == null) {
                                    throw new RuntimeException(String.format("There is no mappedBy field '%s' in the table class '%s'.", mappedBy, targetEntity.getSimpleName()));
                                }

                                // Check if the detail column is a foreign key
                                if (mappedByField.isAnnotationPresent(ForeignKey.class)) {
                                    ForeignKey foreignKeyAnnotation = mappedByField.getAnnotation(ForeignKey.class);

                                    // Detail column is the column specified in the relationship annotation
                                    detailColumnName = UtilsReflection.getColumnName(mappedByField);
                                    detailColumnField = mappedByField;

                                    // Master column is the master column specified in foreign key annotation
                                    masterColumnName = foreignKeyAnnotation.referencedColumnName();
                                    if (TextUtils.isEmpty(masterColumnName)) {
                                        masterColumnName = UtilsReflection.getColumnName(mappedByField);
                                    }
                                } else {
                                    throw new RuntimeException(String.format("MappedBy field '%s' in the table class '%s' should be annotated with %s.", mappedBy, targetEntity.getSimpleName(), ForeignKey.class.getSimpleName()));
                                }

                                // Master table is the table currently being processed
                                masterTableName = tableName;

                                // Detail table name is the table specified in the relationship annotation
                                detailTableName = UtilsReflection.getTableName(targetEntity);
                            }
                        } else if (!isForeignKey) {
                            throw new RuntimeException(String.format("Field '%s' in the class '%s' is not a foreign key, so it must specify mappedBy in its relationship annotation.", selectableColumnField.getName(), pTableClass.getSimpleName()));
                        }

                        // Process foreign key annotation if there is one
                        if (isForeignKey) {
                            // This column is a foreign key

                            ForeignKey foreignKeyAnnotation = selectableColumnField.getAnnotation(ForeignKey.class);

                            // Master table is the table to be joint
                            masterTableName = UtilsReflection.getTableName(columnDataType);
                            masterTableClass = columnDataType;

                            // Master column is the column in the master table
                            masterColumnName = foreignKeyAnnotation.referencedColumnName();
                            if (TextUtils.isEmpty(masterColumnName)) {
                                masterColumnName = columnName;
                            }

                            // Check if the master column exists in the master table
                            masterColumnField = UtilsReflection.getColumnFieldByColumnName(masterColumnName, masterTableClass);
                            if (masterColumnField == null) {
                                throw new RuntimeException(String.format("There is no column field '%s' in the table class '%s'", masterColumnName, columnDataType.getSimpleName()));
                            }

                            // Detail table is the table currently being processed
                            detailTableName = tableName;
                            detailTableClass = pTableClass;
                            //targetEntity = pTableClass;

                            // Detail column this the column currently being processed
                            detailColumnName = columnName;
                            detailColumnField = selectableColumnField;
                        }

                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(masterTableName);
                        stringBuilder.append(".");
                        stringBuilder.append(masterColumnName);
                        stringBuilder.append(" = ");
                        stringBuilder.append(detailTableName);
                        stringBuilder.append(".");
                        stringBuilder.append(detailColumnName);
                        String joinClause = stringBuilder.toString();

                        // Check if this join is already processed
                        if (pTableClause.indexOf(joinClause) > -1) {
                            continue;
                        }

                        UtilsQuery.buildProjectionsAndTableClause(pProjections, pTableClause, columnDataType, pAliasHashMap);
                    }
                }
            }
        } else {
            throw new RuntimeException(String.format("%s is not annotated with %s", pTableClass.getSimpleName(), Table.class.getSimpleName()));
        }
    }
}
