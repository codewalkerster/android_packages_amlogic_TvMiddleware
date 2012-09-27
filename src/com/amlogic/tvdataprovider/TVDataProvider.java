package com.amlogic.tvdataprovider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.content.UriMatcher;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.File;
import com.amlogic.tvutil.TVChannelParams;

public class TVDataProvider extends ContentProvider{
	private static final String TAG = "TVDataProvider";
	private static final String DB_NAME = "dvb.db";
	private static final String AUTHORITY = "com.amlogic.tvdataprovider.TVDataProvider";
	private static final String DB_VERSION_FIELD = "DATABASE_VERSION";
	private static final int RD_SQL = 1;
	private static final int WR_SQL = 2;
	private static final UriMatcher URI_MATCHER;
	
	public static final Uri RD_URL = Uri.parse("content://com.amlogic.tv.tvdataprovider/rd_db");
	public static final Uri WR_URL = Uri.parse("content://com.amlogic.tv.tvdataprovider/wr_db");

	private static TVDatabase db = null;
	private static int openCount = 0;

	static{
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(TVDataProvider.AUTHORITY, "rd_db", RD_SQL);
		URI_MATCHER.addURI(TVDataProvider.AUTHORITY, "wr_db", WR_SQL);
	}

	private static class TVRegion {
		private String name;
		private int source;
		private String freqList;

		public TVRegion(String rn, int src, String fl) {
			name = rn;
			source = src;
			freqList = fl;
		}
	};

	private static final TVRegion tvRegions[] = {
		new TVRegion("Default Allband", TVChannelParams.MODE_QAM, "474000000 490000000"),
	};

	public synchronized static void openDatabase(Context context){
		if(openCount == 0){
			Log.d(TAG, "open database");
			String path;
			SharedPreferences pref;
			Log.d(TAG, "111");
			db = new TVDatabase(context, null);
			Log.d(TAG, "2222");
			TVDatabase fileDB = new TVDatabase(context, DB_NAME);
			path = new String(fileDB.getReadableDatabase().getPath());
			fileDB.close();
			Log.d(TAG, "33333");
			/*Check the database version.*/
			pref = PreferenceManager.getDefaultSharedPreferences(context);
			int curVer = pref.getInt(DB_VERSION_FIELD, -1);
			if(curVer != TVDatabase.DB_VERSION){
				File tmpFile = new File(path);
				pref.edit().putInt(DB_VERSION_FIELD, TVDatabase.DB_VERSION).commit();

				fileDB = new TVDatabase(context, DB_NAME);
				fileDB.close();
			}
			Log.d(TAG, "4444");
			db.getWritableDatabase().execSQL("attach database '"+path+"' as filedb");
			db.getWritableDatabase().execSQL("insert into net_table select * from filedb.net_table");
			db.getWritableDatabase().execSQL("insert into ts_table select  * from filedb.ts_table");
			db.getWritableDatabase().execSQL("insert into srv_table select * from filedb.srv_table");
			db.getWritableDatabase().execSQL("insert into rec_table select * from filedb.rec_table");
			db.getWritableDatabase().execSQL("insert into grp_table select * from filedb.grp_table");
			db.getWritableDatabase().execSQL("insert into evt_table select * from filedb.evt_table");
			db.getWritableDatabase().execSQL("insert into subtitle_table select * from filedb.subtitle_table");
			db.getWritableDatabase().execSQL("insert into teletext_table select * from filedb.teletext_table");
			db.getWritableDatabase().execSQL("insert into dimension_table select * from filedb.dimension_table");
			db.getWritableDatabase().execSQL("insert into sat_para_table select * from filedb.sat_para_table");
			Log.d(TAG, "5555");
			/** load the frequency lists from code*/
			ContentValues cv = new ContentValues();
			for (int i=0; i<tvRegions.length; i++) {
				Log.d(TAG, "Loading region "+tvRegions[i].name+", source "+tvRegions[i].source);
				cv.put("name", tvRegions[i].name);
				cv.put("source", tvRegions[i].source);
				cv.put("frequencies", tvRegions[i].freqList);
				db.getWritableDatabase().insert("region_table", "", cv);
				Log.d(TAG, tvRegions[i].name + "done !");
				cv.clear();
			}
			Log.d(TAG, "provider open database done");
		}

		openCount++;
	}

	public static synchronized void syncToFile(){
		if(db == null)
			return;

		db.getWritableDatabase().execSQL("drop table if exists filedb.net_table");
		db.getWritableDatabase().execSQL("drop table if exists filedb.ts_table");
		db.getWritableDatabase().execSQL("drop table if exists filedb.srv_table");
		db.getWritableDatabase().execSQL("drop table if exists filedb.grp_table");
		db.getWritableDatabase().execSQL("drop table if exists filedb.grp_map_table");
		db.getWritableDatabase().execSQL("drop table if exists filedb.subtitle_table");
		db.getWritableDatabase().execSQL("drop table if exists filedb.teletext_table");
		db.getWritableDatabase().execSQL("drop table if exists filedb.evt_table");
		db.getWritableDatabase().execSQL("drop table if exists filedb.dimension_table");
		db.getWritableDatabase().execSQL("drop table if exists filedb.sat_para_table");

		/*Store data from memory to file*/
		db.getWritableDatabase().execSQL("create table filedb.net_table as select * from net_table");
		db.getWritableDatabase().execSQL("create table filedb.ts_table as select * from ts_table");
		db.getWritableDatabase().execSQL("create table filedb.srv_table as select * from srv_table");
		db.getWritableDatabase().execSQL("create table filedb.grp_table as select * from grp_table");
		db.getWritableDatabase().execSQL("create table filedb.grp_map_table as select * from grp_map_table");
		db.getWritableDatabase().execSQL("create table filedb.subtitle_table as select * from subtitle_table");
		db.getWritableDatabase().execSQL("create table filedb.teletext_table as select * from teletext_table");
		db.getWritableDatabase().execSQL("create table filedb.evt_table as select * from evt_table where sub_flag!=0");
		db.getWritableDatabase().execSQL("create table filedb.dimension_table as select * from dimension_table");
		db.getWritableDatabase().execSQL("create table filedb.sat_para_table as select * from sat_para_table");
	}

	public synchronized static void closeDatabase(Context context){
		if(openCount <= 0)
			return;

		openCount--;

		if((openCount == 0) && (db != null)){
			Log.d(TAG, "close database");
			db.getWritableDatabase().execSQL("detach database filedb");
			db.close();
			db = null;
		}
	}

	public synchronized static int getDatabaseNativeHandle() {
		if (db == null) {
			return 0;
		}
		return db.getNativeHandle();
	}

	@Override
	public boolean onCreate()
	{
		openDatabase(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri)
	{
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		int id = URI_MATCHER.match(uri);
		Cursor c = null;

		if(id == RD_SQL){
			c = db.getReadableDatabase().rawQuery(selection, null);
		}else if(id == WR_SQL){
			db.getWritableDatabase().execSQL(selection);
		}

		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues)
	{
		return null;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs)
	{
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values,  String where, String[] whereArgs)
	{
		return 0;
	}
}

