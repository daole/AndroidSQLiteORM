package com.dreamdigitizers.androidsqliteorm.utilities;

import java.util.HashMap;

public class UtilsNaming {
    public static String buildTableAlias(Class pTableClass, HashMap<Class, Integer> pAliasHashMap) {
        Integer number;
        if (pAliasHashMap.containsKey(pTableClass)) {
            number = pAliasHashMap.get(pTableClass);
            number++;
        } else {
            number = 0;
        }
        pAliasHashMap.put(pTableClass, number);
        return UtilsNaming.buildTableAlias(pTableClass, number);
    }

    public static String buildTableAlias(Class pTableClass, Integer pNumber) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(UtilsReflection.getTableName(pTableClass));
        stringBuilder.append(pNumber.toString());
        String alias = stringBuilder.toString();
        return alias;
    }

    public static void decreaseTableAliasNumber(Class pTableClass, HashMap<Class, Integer> pAliasHashMap) {
        if (pAliasHashMap.containsKey(pTableClass)) {
            Integer number = pAliasHashMap.get(pTableClass);
            number--;
            pAliasHashMap.put(pTableClass, number);
        }
    }

    public static String buildColumnAlias(String pTableName, String pColumnName) {
        return UtilsNaming.buildColumnAlias(pTableName, pColumnName, true);
    }

    public static String buildColumnAlias(String pTableName, String pColumnName, boolean pIsIncludeAsClause) {
        StringBuilder stringBuilder = new StringBuilder();

        if (pIsIncludeAsClause) {
            stringBuilder.append(pTableName);
            stringBuilder.append(".");
            stringBuilder.append(pColumnName);
            stringBuilder.append(" AS ");
        }
        stringBuilder.append(pTableName);
        stringBuilder.append("_");
        stringBuilder.append(pColumnName);

        String alias = stringBuilder.toString();
        return alias;
    }
}
