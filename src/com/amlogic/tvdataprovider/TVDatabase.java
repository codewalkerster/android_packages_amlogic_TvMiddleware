package com.amlogic.tvdataprovider;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;

public class TVDatabase
{
    public static final int DB_VERSION = 1;
    private int native_handle;
    private DatabaseHelper helper;
    private SQLiteDatabase readableDB;
    private SQLiteDatabase writableDB;

    /*implemented by libjnidvbdatabase.so*/
    private native int native_db_init(SQLiteDatabase db);

    /*Load native library*/
    static {
        System.loadLibrary("jnitvdatabase");
    }

    public TVDatabase(Context context, String dbName) {
        helper = new DatabaseHelper(context, dbName);
        writableDB = helper.getWritableDatabase();
        readableDB = helper.getReadableDatabase();
        native_handle = native_db_init(writableDB);
    }

    public int getNativeHandle() {
        return (helper!=null) ? native_handle : 0;
    }

    class DatabaseHelper extends SQLiteOpenHelper
    {
        DatabaseHelper(Context context, String name) {
            super(context, name, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public SQLiteDatabase getReadableDatabase() {
        return (helper == null) ? null : readableDB;
    }

    public SQLiteDatabase getWritableDatabase() {
        return (helper == null) ? null : writableDB;
    }

    public void close() {
        if(helper != null) {
            helper.close();
            helper = null;
        }
    }
}

