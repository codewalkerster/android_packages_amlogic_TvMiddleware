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
	private static final String AUTHORITY = "com.amlogic.tv.tvdataprovider";
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
		/* default DVB-C */
		new TVRegion("Default DVB-C", TVChannelParams.MODE_QAM, 
					"52500000 60250000 68500000 80000000 88000000 115000000 123000000 131000000 139000000 147000000 155000000 163000000 "+
					"171000000 179000000 187000000 195000000 203000000 211000000 219000000 227000000 235000000 243000000 251000000 259000000 "+
					"267000000 275000000 283000000 291000000 299000000 307000000 315000000 323000000 331000000 339000000 347000000 355000000 "+
					"363000000 371000000 379000000 387000000 395000000 403000000 411000000 419000000 427000000 435000000 443000000 451000000 "+
					"459000000 467000000 474000000 482000000 490000000 498000000 506000000 514000000 522000000 530000000 538000000 546000000 "+
					"554000000 562000000 570000000 578000000 586000000 594000000 602000000 610000000 618000000 626000000 634000000 642000000 "+
					"650000000 658000000 666000000 674000000 682000000 690000000 698000000 706000000 714000000 722000000 730000000 738000000 "+
					"746000000 754000000 762000000 770000000 778000000 786000000 794000000 802000000 810000000 818000000 826000000 834000000 "+
					"842000000 850000000 858000000 866000000 874000000"),
	};

	public synchronized static void openDatabase(Context context){
		if(openCount == 0){
			String path;
			SharedPreferences pref;
			
			db = new TVDatabase(context, null);
			TVDatabase fileDB = new TVDatabase(context, DB_NAME);
			path = new String(fileDB.getReadableDatabase().getPath());
			fileDB.close();
			/*Check the database version.*/
			pref = PreferenceManager.getDefaultSharedPreferences(context);
			int curVer = pref.getInt(DB_VERSION_FIELD, -1);
			if(curVer != TVDatabase.DB_VERSION){
				File tmpFile = new File(path);
				pref.edit().putInt(DB_VERSION_FIELD, TVDatabase.DB_VERSION).commit();

				fileDB = new TVDatabase(context, DB_NAME);
				fileDB.close();
			}
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
			/** load the frequency lists from code*/
			ContentValues cv = new ContentValues();
			for (int i=0; i<tvRegions.length; i++) {
				Log.d(TAG, "Loading region "+tvRegions[i].name+", source "+tvRegions[i].source);
				cv.put("name", tvRegions[i].name);
				cv.put("source", tvRegions[i].source);
				cv.put("frequencies", tvRegions[i].freqList);
				db.getWritableDatabase().insert("region_table", "", cv);
				Log.d(TAG, tvRegions[i].name + " done !");
				cv.clear();
			}
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

