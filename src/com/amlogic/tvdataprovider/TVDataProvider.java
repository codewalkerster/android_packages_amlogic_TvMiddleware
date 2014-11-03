package com.amlogic.tvdataprovider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.content.UriMatcher;
import android.util.Log;
import java.io.File;
import com.amlogic.tvservice.TVConfig;
import com.amlogic.tvutil.TVConfigValue;


public class TVDataProvider extends ContentProvider{
	private static final String TAG = "TVDataProvider";
	private static final String DB_NAME = "dvb.db";
	private static final String AUTHORITY = "com.amlogic.tv.tvdataprovider";
	private static final String DB_PATH = "/data/data/com.amlogic.tvservice/databases/dvb.db";
	private static final String DB_BAK_PATH = "/data/data/com.amlogic.tvservice/databases/dvb.db.bak";
	private static final int RD_SQL = 1;
	private static final int WR_SQL = 2;
	private static final int RD_CONFIG = 3;
	private static final int WR_CONFIG = 4;
	private static final UriMatcher URI_MATCHER;
	private static final int FORMAT_OK = 0;
	private static final int MINOR_FORMAT_ERROR = 1;
	private static final int MAJOR_FORMAT_ERROR = 2;
	private static final int FORMAT_ERROR = 3;

	public static final Uri RD_URL = Uri.parse("content://com.amlogic.tv.tvdataprovider/rd_db");
	public static final Uri WR_URL = Uri.parse("content://com.amlogic.tv.tvdataprovider/wr_db");
	public static final Uri RD_CONFIG_URL = Uri.parse("content://com.amlogic.tv.tvdataprovider/rd_config");
	public static final Uri WR_CONFIG_URL = Uri.parse("content://com.amlogic.tv.tvdataprovider/wr_config");

	private static int openCount = 0;
	private static TVDatabase db;
	private static boolean modified = false;
	private static TVConfig config;
	private static TVDatabaseErrorHandler errorHandle;


	private native static int native_db_check();
	static{
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(TVDataProvider.AUTHORITY, "rd_db", RD_SQL);
		URI_MATCHER.addURI(TVDataProvider.AUTHORITY, "wr_db", WR_SQL);
		URI_MATCHER.addURI(TVDataProvider.AUTHORITY, "rd_config", RD_CONFIG);
		URI_MATCHER.addURI(TVDataProvider.AUTHORITY, "wr_config", WR_CONFIG);
	}


	public synchronized static void openDatabase(Context context, TVConfig cfg){
		if(openCount == 0){
			errorHandle = new TVDatabaseErrorHandler();
			int db_check = native_db_check();
			//String dbName = new String(DB_PATH);
			//String dbBakName = new String(DB_BAK_PATH);
			if(db_check == FORMAT_OK) {
				Log.d(TAG, "*************check db ok, do nothing");
			}else if(db_check == MINOR_FORMAT_ERROR) {
				Log.d(TAG, "*************check db_bak is error, do backup");
				errorHandle.copyFile(DB_PATH, DB_BAK_PATH);
			}else if(db_check == MAJOR_FORMAT_ERROR) {
				Log.d(TAG, "*************check db is error, do recovery");
				errorHandle.copyFile(DB_BAK_PATH, DB_PATH);
			}else if(db_check == FORMAT_ERROR) {
				Log.d(TAG, "*************check db&bak is error, do delete");
				errorHandle.deleteDatabaseFile(DB_PATH);
				errorHandle.deleteDatabaseFile(DB_BAK_PATH);
			}
			db = new TVDatabase(context, DB_NAME, errorHandle);
			modified = true;
		}
		if (cfg != null){
			config = cfg;
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

	public synchronized static void restore(Context context){
		db.getWritableDatabase().execSQL("delete from net_table");
		db.getWritableDatabase().execSQL("delete from ts_table");
		db.getWritableDatabase().execSQL("delete from srv_table");
		db.getWritableDatabase().execSQL("delete from evt_table");
		db.getWritableDatabase().execSQL("delete from booking_table");
		db.getWritableDatabase().execSQL("delete from grp_table");
		db.getWritableDatabase().execSQL("delete from grp_map_table");
		db.getWritableDatabase().execSQL("delete from dimension_table");
		db.getWritableDatabase().execSQL("delete from sat_para_table");
		db.getWritableDatabase().execSQL("delete from region_table");
		modified = true;

		/*load all builtin data*/
		db.loadBuiltins(context);
	}

	/*Load native library*/
    static {
        System.loadLibrary("jnitvdbcheck");
    }

	public synchronized static void importDatabase(Context context, String inputXmlPath) throws Exception{
		db.importFromXml(context,inputXmlPath);
		modified = true;
	}

	public synchronized static void exportDatabase(Context context, String outputXmlPath) throws Exception{
		db.exportToXml(context,outputXmlPath);
	}

	@Override
	public boolean onCreate()
	{
		openDatabase(getContext(), null);
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
		}else if (id == RD_CONFIG){
			String strType;

			if (selection == null || selectionArgs == null ||
				selectionArgs.length < 1 || config == null){
				Log.d(TAG, "Cannot read config, invalid args.");
				return c;
			}

			/* parse the config type */
			strType = selectionArgs[0];
			if (!strType.equalsIgnoreCase("Int") &&
				!strType.equalsIgnoreCase("String") &&
				!strType.equalsIgnoreCase("Boolean")){
				Log.d(TAG, "Invalid value type, must in (Int, String, Boolean)");
				return c;
			}

			/* get value from config and add it to a cursor */
			String[] strCur = new String[] {"value"};
			MatrixCursor mc = new MatrixCursor(strCur);

			try{
				if (strType.equalsIgnoreCase("Int")){
					mc.addRow(new Object[] {config.getInt(selection)});
				}else if (strType.equalsIgnoreCase("String")){
					mc.addRow(new Object[] {config.getString(selection)});
				}else if (strType.equalsIgnoreCase("Boolean")){
					mc.addRow(new Object[] {config.getBoolean(selection) ? 1 : 0});
				}
			}catch (Exception e){

			}

			c = mc;
		}else if (id == WR_CONFIG && selection != null){
			String strType, strValue;

			if (selection == null || selectionArgs == null ||
				selectionArgs.length < 2 || config == null){
				Log.d(TAG, "Cannot write config, invalid args.");
				return c;
			}

			/* parse the config type */
			strType = selectionArgs[0];
			if (!strType.equalsIgnoreCase("Int") &&
				!strType.equalsIgnoreCase("String") &&
				!strType.equalsIgnoreCase("Boolean")){
				Log.d(TAG, "Invalid value type, must in (Int, String, Boolean)");
				return c;
			}

			/* set to config */
			strValue = selectionArgs[1];
			try{
				if (strType.equalsIgnoreCase("Int")){
					config.set(selection, new TVConfigValue(Integer.parseInt(strValue)));
				}else if (strType.equalsIgnoreCase("String")){
					config.set(selection, new TVConfigValue(strValue));
				}else if (strType.equalsIgnoreCase("Boolean")){
					config.set(selection, new TVConfigValue((Integer.parseInt(strValue)==0) ? false : true));
				}
			}catch (Exception e){

			}
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

	public static boolean backUpDatabase(){
		//String dbName = new String(DB_PATH);
		//String dbBakName = new String("/data/data/com.amlogic.tvservice/databases/dvb.db.bak");
		return errorHandle.copyFile(DB_PATH, DB_BAK_PATH);
	}
}

