package com.dreamdigitizers.androidsqliteorm.utilities;

public class UtilsNaming {
    public static String buildAlias(String pTableName, String pColumnName) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(pTableName);
        stringBuilder.append(".");
        stringBuilder.append(pColumnName);
        stringBuilder.append(" as ");
        stringBuilder.append(pTableName);
        stringBuilder.append("_");
        stringBuilder.append(pColumnName);

        String alias = stringBuilder.toString();
        return alias;
    }
}
