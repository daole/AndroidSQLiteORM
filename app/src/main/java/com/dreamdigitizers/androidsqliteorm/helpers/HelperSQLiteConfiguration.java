package com.dreamdigitizers.androidsqliteorm.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dreamdigitizers.androidsqliteorm.utilities.UtilsDatabaseSchema;
import com.dreamdigitizers.androidsqliteorm.utilities.UtilsReflection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HelperSQLiteConfiguration extends SQLiteOpenHelper {
    private SQLiteConfigurationInformation mSQLiteConfigurationInformation;

    public HelperSQLiteConfiguration(SQLiteConfigurationInformation pSQLiteConfigurationInformation) {
        super(pSQLiteConfigurationInformation.getDatabaseContext(),
                pSQLiteConfigurationInformation.getDatabaseName(),
                pSQLiteConfigurationInformation.getCursorFactory(),
                pSQLiteConfigurationInformation.getDatabaseVersion());
        this.mSQLiteConfigurationInformation = pSQLiteConfigurationInformation;
    }

    @Override
    public void onConfigure(SQLiteDatabase pSQLiteDatabase) {
        long databaseMaximumSize = this.mSQLiteConfigurationInformation.getDatabaseMaximumSize();
        if (databaseMaximumSize > 0) {
            pSQLiteDatabase.setMaximumSize(databaseMaximumSize);
        }

        long databasePageSize = this.mSQLiteConfigurationInformation.getDatabasePageSize();
        if (databasePageSize > 0) {
            pSQLiteDatabase.setPageSize(databasePageSize);
        }

        Locale databaseLocale = this.mSQLiteConfigurationInformation.getDatabaseLocale();
        if (databaseLocale != null) {
            pSQLiteDatabase.setLocale(databaseLocale);
        }

        super.onConfigure(pSQLiteDatabase);
    }

    @Override
    public void onCreate(SQLiteDatabase pSQLiteDatabase) {
        UtilsDatabaseSchema.createDatabaseSchema(this.mSQLiteConfigurationInformation.getTableClasses(), pSQLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase pSQLiteDatabase, int pOldVersion, int pNewVersion) {
        UtilsDatabaseSchema.updateDatabaseSchema(this.mSQLiteConfigurationInformation.getTableClasses(), pSQLiteDatabase);
    }

    public static class SQLiteConfigurationInformation {
        private Context mDatabaseContext;
        private String mDatabaseName;
        private SQLiteDatabase.CursorFactory mCursorFactory;
        private int mDatabaseVersion;
        private long mDatabaseMaximumSize;
        private long mDatabasePageSize;
        private Locale mDatabaseLocale;
        private List<Class<?>> mTableClasses;

        public SQLiteConfigurationInformation() {
            this.mTableClasses = new ArrayList<>();
        }

        public Context getDatabaseContext() {
            return this.mDatabaseContext;
        }

        public void setDatabaseContext(Context pDatabaseContext) {
            this.mDatabaseContext = pDatabaseContext;
        }

        public String getDatabaseName() {
            return this.mDatabaseName;
        }

        public void setDatabaseName(String pDatabaseName) {
            this.mDatabaseName = pDatabaseName;
        }

        public SQLiteDatabase.CursorFactory getCursorFactory() {
            return this.mCursorFactory;
        }

        public void setCursorFactory(SQLiteDatabase.CursorFactory pCursorFactory) {
            this.mCursorFactory = pCursorFactory;
        }

        public int getDatabaseVersion() {
            return this.mDatabaseVersion;
        }

        public void setDatabaseVersion(int pDatabaseVersion) {
            this.mDatabaseVersion = pDatabaseVersion;
        }

        public long getDatabaseMaximumSize() {
            return this.mDatabaseMaximumSize;
        }

        public void setDatabaseMaxSize(long pDatabaseMaximumSize) {
            this.mDatabaseMaximumSize = pDatabaseMaximumSize;
        }

        public long getDatabasePageSize() {
            return this.mDatabasePageSize;
        }

        public void setDatabasePageSize(int pDatabasePageSize) {
            this.mDatabasePageSize = pDatabasePageSize;
        }

        public Locale getDatabaseLocale() {
            return this.mDatabaseLocale;
        }

        public void setDatabaseLocale(Locale pDatabaseLocale) {
            this.mDatabaseLocale = pDatabaseLocale;
        }

        public List<Class<?>> getTableClasses() {
            return this.mTableClasses;
        }

        public void setTableClasses(List<Class<?>> pTableClasses) {
            this.mTableClasses = pTableClasses;
        }

        public void setTableClasses(Class<?>... pTableClasses) {
            for (Class<?> tableClass : pTableClasses) {
                if (UtilsReflection.isTableClass(tableClass)) {
                    this.mTableClasses.add(tableClass);
                }
            }
        }
    }
}
