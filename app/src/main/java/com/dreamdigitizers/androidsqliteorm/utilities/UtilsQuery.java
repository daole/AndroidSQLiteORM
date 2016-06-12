package com.dreamdigitizers.androidsqliteorm.utilities;

import android.text.TextUtils;

import com.dreamdigitizers.androidsqliteorm.FetchType;
import com.dreamdigitizers.androidsqliteorm.annotations.ForeignKey;
import com.dreamdigitizers.androidsqliteorm.annotations.ManyToOne;
import com.dreamdigitizers.androidsqliteorm.annotations.OneToMany;
import com.dreamdigitizers.androidsqliteorm.annotations.OneToOne;
import com.dreamdigitizers.androidsqliteorm.annotations.Table;

import java.lang.reflect.Field;
import java.util.List;

public class UtilsQuery {
    public static <T> void buildProjectionsAndTableClause(List<String> pProjections, StringBuilder pTableClauseBuilder, Class<T> pTableClass) {
        pProjections.clear();
        pTableClauseBuilder.setLength(0);
        UtilsQuery.buildProjectionsAndTableClause(pProjections, pTableClauseBuilder, pTableClass, false, null);
    }

    public static <T> void buildProjectionsAndTableClause(List<String> pProjections, StringBuilder pTableClause, Class<T> pTableClass, boolean pIsOptional, String pJoinClause) {
        if (UtilsReflection.isTableClass(pTableClass)) {
            String tableName = UtilsReflection.getTableName(pTableClass);
            boolean isJoin = false;

            if (pTableClause.length() != 0) {
                isJoin = true;

                if (pIsOptional) {
                    pTableClause.append(" LEFT");
                }

                pTableClause.append(" JOIN ");
            }

            pTableClause.append(tableName);

            if (isJoin) {
                pTableClause.append(" ON ");
                pTableClause.append(pJoinClause);
            }

            List<Field> selectableColumnFields = UtilsReflection.getAllSelectableColumnFields(pTableClass);
            for (Field selectableColumnField : selectableColumnFields) {
                String columnName = UtilsReflection.getColumnName(selectableColumnField);
                Class<?> columnDataType = selectableColumnField.getType();

                if (columnDataType.isPrimitive()) {
                    // Process normal column
                    String alias = UtilsNaming.buildAlias(tableName, columnName);
                    pProjections.add(alias);
                } else {
                    // Process column referencing to another table

                    // Check if this column references to a valid table
                    if (!UtilsReflection.isTableClass(columnDataType)) {
                        throw new RuntimeException(String.format("%s is not annotated with %s.", columnDataType.getSimpleName(), Table.class.getSimpleName()));
                    }

                    boolean isOptional;
                    Class<?> detailTableClass;
                    String detailColumnFieldName;
                    FetchType fetchType;

                    // Retrieve relationship information
                    if (selectableColumnField.isAnnotationPresent(OneToOne.class)) {
                        OneToOne oneToOneAnnotation = selectableColumnField.getAnnotation(OneToOne.class);

                        isOptional = oneToOneAnnotation.isOptional();
                        detailTableClass = oneToOneAnnotation.detailTableClass();
                        detailColumnFieldName = oneToOneAnnotation.detailColumnFieldName();
                        fetchType = oneToOneAnnotation.fetchType();
                    } else if (selectableColumnField.isAnnotationPresent(OneToMany.class)) {
                        OneToMany oneToManyAnnotation = selectableColumnField.getAnnotation(OneToMany.class);

                        isOptional = oneToManyAnnotation.isOptional();
                        detailTableClass = oneToManyAnnotation.detailTableClass();
                        detailColumnFieldName = oneToManyAnnotation.detailColumnFieldName();
                        fetchType = oneToManyAnnotation.fetchType();
                    } else if (selectableColumnField.isAnnotationPresent(ManyToOne.class)) {
                        ManyToOne manyToOneAnnotation = selectableColumnField.getAnnotation(ManyToOne.class);

                        isOptional = manyToOneAnnotation.isOptional();
                        detailTableClass = manyToOneAnnotation.detailTableClass();
                        detailColumnFieldName = manyToOneAnnotation.detailColumnFieldName();
                        fetchType = manyToOneAnnotation.fetchType();
                    } else {
                        throw new RuntimeException(String.format("Field '%s' in the class '%s' must be annotated with one of relationship annotation.", selectableColumnField.getName(), pTableClass.getSimpleName()));
                    }

                    String masterTableName = null;
                    String masterColumnName = null;
                    String detailTableName = null;
                    String detailColumnName = null;

                    // Process foreign key annotation if there is one
                    boolean isForeignKey = selectableColumnField.isAnnotationPresent(ForeignKey.class);
                    if (isForeignKey) {
                        // This column is a foreign key

                        ForeignKey foreignKeyAnnotation = selectableColumnField.getAnnotation(ForeignKey.class);

                        // Master table is the table to be joint
                        masterTableName = UtilsReflection.getTableName(columnDataType);

                        // Master column is the column in the master table
                        masterColumnName = foreignKeyAnnotation.masterColumnName();
                        if (TextUtils.isEmpty(masterColumnName)) {
                            masterColumnName = columnName;
                        }

                        // Check if the master column exists in the master table
                        Field masterColumnField = UtilsReflection.getColumnFieldByColumnName(masterColumnName, columnDataType);
                        if (masterColumnField == null) {
                            throw new RuntimeException(String.format("There is no column field '%s' in the table class '%s'", masterColumnName, columnDataType.getSimpleName()));
                        }

                        // Detail table is the table currently being processed
                        detailTableName = tableName;

                        // Detail column this the column currently being processed
                        detailColumnName = columnName;
                    }

                    // Process detail table and detail column if they are specified in the relationship annotation
                    if (detailTableClass != void.class || !TextUtils.isEmpty(detailColumnFieldName)) {
                        if (isForeignKey) {
                            // If the column being processed is a foreign key, it must not specify detail table and detail column

                            throw new RuntimeException(String.format("Field '%s' in the class '%s' is a foreign key, so it must not specify detail table class and detail column field name in its relationship annotation.", selectableColumnField.getName(), pTableClass.getSimpleName()));
                        } else {
                            // Check if Detail table is specified
                            if (detailTableClass == void.class) {
                                throw new RuntimeException(String.format("Field '%s' in the class '%s' does not specify the detail table class in its relationship annotation", selectableColumnField.getName(), pTableClass.getSimpleName()));
                            }

                            // Check if detail column is specified
                            if (TextUtils.isEmpty(detailColumnFieldName)) {
                                throw new RuntimeException(String.format("Field '%s' in the class '%s' does not specify the detail column field name in its relationship annotation", selectableColumnField.getName(), pTableClass.getSimpleName()));
                            }

                            // Check if the detail column exists in the detail table
                            Field detailColumnField = UtilsReflection.getColumnFieldByColumnName(detailColumnFieldName, detailTableClass);
                            if (detailColumnField == null) {
                                throw new RuntimeException(String.format("There is no detail column field '%s' in the detail table class '%s'.", detailColumnFieldName, detailTableClass.getSimpleName()));
                            }

                            // Check if the detail column is a foreign key
                            if (detailColumnField.isAnnotationPresent(ForeignKey.class)) {
                                ForeignKey foreignKeyAnnotation = detailColumnField.getAnnotation(ForeignKey.class);

                                // Detail column is the column specified in the relationship annotation
                                detailColumnName = UtilsReflection.getColumnName(detailColumnField);

                                // Master column is the master column specified in foreign key annotation
                                masterColumnName = foreignKeyAnnotation.masterColumnName();
                                if (TextUtils.isEmpty(masterColumnName)) {
                                    masterColumnName = UtilsReflection.getColumnName(detailColumnField);
                                }
                            } else {
                                throw new RuntimeException(String.format("Detail column field '%s' in the detail table class '%s' should be annotated with %s.", detailColumnFieldName, detailTableClass.getSimpleName(), ForeignKey.class.getSimpleName()));
                            }

                            // Master table is the table currently being processed
                            masterTableName = tableName;

                            // Detail table name is the table specified in the relationship annotation
                            detailTableName = UtilsReflection.getTableName(detailTableClass);
                        }
                    } else if (!isForeignKey) {
                        throw new RuntimeException(String.format("Field '%s' in the class '%s' is not a foreign key, so it must specify detail table class and detail column field name in its relationship annotation.", selectableColumnField.getName(), pTableClass.getSimpleName()));
                    }

                    if (fetchType == FetchType.EAGER) {
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

                        UtilsQuery.buildProjectionsAndTableClause(pProjections, pTableClause, columnDataType, isOptional, joinClause);
                    }
                }
            }
        } else {
            throw new RuntimeException(String.format("%s is not annotated with %s", pTableClass.getSimpleName(), Table.class.getSimpleName()));
        }
    }
}
