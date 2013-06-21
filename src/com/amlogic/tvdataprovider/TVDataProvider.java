package com.amlogic.tvdataprovider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.content.UriMatcher;
import android.util.Log;
import java.io.File;

public class TVDataProvider extends ContentProvider{
	private static final String TAG = "TVDataProvider";
	private static final String DB_NAME = "dvb.db";
	private static final String AUTHORITY = "com.amlogic.tv.tvdataprovider";
	private static final int RD_SQL = 1;
	private static final int WR_SQL = 2;
	private static final UriMatcher URI_MATCHER;
	
	public static final Uri RD_URL = Uri.parse("content://com.amlogic.tv.tvdataprovider/rd_db");
	public static final Uri WR_URL = Uri.parse("content://com.amlogic.tv.tvdataprovider/wr_db");

	private static int openCount = 0;
	private static TVDatabase db;
	private static boolean modified = false;

	static{
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(TVDataProvider.AUTHORITY, "rd_db", RD_SQL);
		URI_MATCHER.addURI(TVDataProvider.AUTHORITY, "wr_db", WR_SQL);
	}


	public synchronized static void openDatabase(Context context){
		if(openCount == 0){
			db = new TVDatabase(context, DB_NAME);
			modified = true;


		}

		openCount++;
	}

	public synchronized static void closeDatabase(Context context){
		if(openCount <= 0)
			return;

		openCount--;

		if(openCount == 0){
			Log.d(TAG, "close database");
			db.unsetup(context);
			db.close();
		}
	}

	public synchronized static void restore(){
		db.getWritableDatabase().execSQL("delete from net_table");
		db.getWritableDatabase().execSQL("delete from ts_table");
		db.getWritableDatabase().execSQL("delete from srv_table");
		db.getWritableDatabase().execSQL("delete from evt_table");
		db.getWritableDatabase().execSQL("delete from booking_table");
		db.getWritableDatabase().execSQL("delete from grp_table");
		db.getWritableDatabase().execSQL("delete from grp_map_table");
		db.getWritableDatabase().execSQL("delete from subtitle_table");
		db.getWritableDatabase().execSQL("delete from teletext_table");
		db.getWritableDatabase().execSQL("delete from dimension_table");
		db.getWritableDatabase().execSQL("delete from sat_para_table");
		db.getWritableDatabase().execSQL("delete from region_table");
		modified = true;
		
		/*load all builtin data*/
		db.loadBuiltins();
	}

	public synchronized static void importDatabase(Context context, String inputXmlPath) throws Exception{
		db.importFromXml(inputXmlPath);
		modified = true;
	}

	public synchronized static void exportDatabase(Context context, String outputXmlPath) throws Exception{
		db.exportToXml(outputXmlPath);
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
			modified = true;
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

	public static synchronized void sync(){
		if(modified){
			modified = false;
			db.sync();
		}
	}
}

