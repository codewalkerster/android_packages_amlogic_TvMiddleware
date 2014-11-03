package com.amlogic.tvdataprovider;

import java.io.File;
import java.util.List;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.database.DatabaseErrorHandler;
import android.database.DefaultDatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.util.Pair;

/**
 * TvMiddleware class used to define the actions to take when the database corruption is reported
 * by sqlite.
 */
public final class TVDatabaseErrorHandler implements DatabaseErrorHandler {

    private static final String TAG = "TVDatabaseErrorHandler";

    public void onCorruption(SQLiteDatabase dbObj) {
        Log.e(TAG, "###TvMiddleWare Corruption reported : " + dbObj.getPath());

        // is the corruption detected even before database could be 'opened'?
        if (!dbObj.isOpen()) {
            // database files are not even openable. delete this database file.
            // NOTE if the database has attached databases, then any of them could be corrupt.
            // and not deleting all of them could cause corrupted database file to remain and
            // make the application crash on database open operation. To avoid this problem,
            // the application should provide its own {@link DatabaseErrorHandler} impl class
            // to delete ALL files of the database (including the attached databases).
            deleteDatabaseFile(dbObj.getPath());
            return;
        }

        List<Pair<String, String>> attachedDbs = null;
        try {
            // Close the database, which will cause subsequent operations to fail.
            // before that, get the attached database list first.
            try {
                attachedDbs = dbObj.getAttachedDbs();
            } catch (SQLiteException e) {
                /* ignore */
            }
            try {
                dbObj.close();
            } catch (SQLiteException e) {
                /* ignore */
            }
        } finally {
            // Delete all files of this corrupt database and/or attached databases
            if (attachedDbs != null) {
                for (Pair<String, String> p : attachedDbs) {
                    deleteDatabaseFile(p.second);
                }
            } else {
                // attachedDbs = null is possible when the database is so corrupt that even
                // "PRAGMA database_list;" also fails. delete the main database file
                deleteDatabaseFile(dbObj.getPath());
            }
        }
    }

    public void deleteDatabaseFile(String fileName) {
        if (fileName.equalsIgnoreCase(":memory:") || fileName.trim().length() == 0) {
            return;
        }
        Log.e(TAG, "TvMiddleware deleting the database file: " + fileName);
        try {
            SQLiteDatabase.deleteDatabase(new File(fileName));
        } catch (Exception e) {
            Log.w(TAG, "delete failed: " + e.getMessage());
        }
    }

	public boolean copyFile(String fileFrom, String fileTo) {
        try {
            FileInputStream in = new java.io.FileInputStream(fileFrom);
            FileOutputStream out = new FileOutputStream(fileTo);
            byte[] bt = new byte[1024];
            int count;
            while ((count = in.read(bt)) > 0) {
                out.write(bt, 0, count);
            }
            in.close();
            out.close();
            return true;
        } catch (IOException ex) {
            return false;
        }
	}
}
