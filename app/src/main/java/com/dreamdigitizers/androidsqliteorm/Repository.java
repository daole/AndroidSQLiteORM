package com.dreamdigitizers.androidsqliteorm;

import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;

import com.dreamdigitizers.androidsqliteorm.helpers.HelperSQLite;
import com.dreamdigitizers.androidsqliteorm.helpers.HelperSQLiteConfiguration;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsQuery;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsReflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

        List<T> list = this.fetchData(pTableClass, cursor);
        return list;
    }

    public <T> T selectByID(Class<T> pTableClass, Object pID) {

        return null;
    }

    private <T> List<T> fetchData(Class<T> pTableClass, Cursor pCursor) {
        List<T> list = new ArrayList<>();

        if (pCursor.moveToFirst()) {
            do {
                List<Field> selectableColumnFields = UtilsReflection.getAllSelectableColumnFields(pTableClass);
                for(Field selectableColumnField : selectableColumnFields) {

                }
            } while (pCursor.moveToNext());
        }

        return list;
    }
}
