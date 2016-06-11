package com.dreamdigitizers.androidsqliteorm.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dreamdigitizers.androidsqliteorm.utilities.UtilsDatabaseSchema;

import java.util.Locale;

public class HelperSQLiteConfiguration extends SQLiteOpenHelper {
    private SQLiteConfigurationInformation mSQLiteConfigurationInformation;

    HelperSQLiteConfiguration(SQLiteConfigurationInformation pSQLiteConfigurationInformation) {
        super(pSQLiteConfigurationInformation.getDatabaseContext(),
                pSQLiteConfigurationInformation.getDatabaseName(),
                null,
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
        UtilsDatabaseSchema.createDatabaseSchema(this.mSQLiteConfigurationInformation.getDatabaseContext(), pSQLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase pSQLiteDatabase, int pOldVersion, int pNewVersion) {
        UtilsDatabaseSchema.updateDatabaseSchema(this.mSQLiteConfigurationInformation.getDatabaseContext(), pSQLiteDatabase);
    }

    public static class SQLiteConfigurationInformation {
        private Context mDatabaseContext;
        private String mDatabaseName;
        private int mDatabaseVersion;
        private long mDatabaseMaximumSize;
        private long mDatabasePageSize;
        private Locale mDatabaseLocale;

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
    }
}
