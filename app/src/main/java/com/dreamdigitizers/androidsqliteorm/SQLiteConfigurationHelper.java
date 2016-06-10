package com.dreamdigitizers.androidsqliteorm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.IOException;
import java.util.Set;

public class SQLiteConfigurationHelper extends SQLiteOpenHelper {
    private Set<Class<?>> mTableClasses;

    public SQLiteConfigurationHelper(Context pContext, String pName, int pVersion) {
        super(pContext, pName, null, pVersion);
        try {
            this.mTableClasses = PackageScanner.scanRequiredClasses(pContext);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase pSQLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase pSQLiteDatabase, int pOldVersion, int pNewVersion) {

    }
}
