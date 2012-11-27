package com.amlogic.tvdataprovider;

import java.io.File;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class TVDatabase extends SQLiteOpenHelper
{
	private static final int DB_VERSION = 1;
	private static final String DB_VERSION_FIELD = "DATABASE_VERSION";

	/*implemented by libjnidvbdatabase.so*/
	private native void native_db_setup(String name, boolean create, SQLiteDatabase db);
	private native void native_db_unsetup();

	/*Load native library*/
	static
	{
		System.loadLibrary("jnitvdatabase");
	}

	public void unsetup(Context context){
		native_db_unsetup();
	}

	public TVDatabase(Context context, String dbName){
		super(context, dbName, null, DB_VERSION);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		int curVer = pref.getInt(DB_VERSION_FIELD, -1);
		boolean create = false;

		File file = context.getDatabasePath(dbName);

		if(curVer != DB_VERSION){
			create = true;
		}

		native_db_setup(file.toString(), create, getWritableDatabase());

		if(create){
			pref.edit().putInt(DB_VERSION_FIELD, DB_VERSION);
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db){
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
	}
}

