package com.amlogic.tvdataprovider;

import java.io.File;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVDimension;
import com.amlogic.tvutil.TVRegion;
import com.amlogic.tvutil.TVDBTransformer;

public class TVDatabase extends SQLiteOpenHelper
{
	private static final String TAG = "TVDatabase";
	private static final String DEFAULT_DB_PATH = "/system/etc/tv_default.xml";
	private static final int DB_VERSION = 8;
	private static final String DB_VERSION_FIELD = "DATABASE_VERSION";

	/*implemented by libjnidvbdatabase.so*/
	private native void native_db_setup(String name, boolean create, SQLiteDatabase db);
	private native void native_db_unsetup();
	private native static void native_db_sync();

	private void insertNewDimension(int region, String regionName, String name, 
	                        int indexj, int[] lock, String[] abbrev, String[] text){
		String cmd = "insert into dimension_table(rating_region,rating_region_name,name,graduated_scale,";
		cmd += "values_defined,index_j,version,abbrev0,text0,locked0,abbrev1,text1,locked1,abbrev2,text2,locked2,";
		cmd += "abbrev3,text3,locked3,abbrev4,text4,locked4,abbrev5,text5,locked5,abbrev6,text6,locked6,";
		cmd += "abbrev7,text7,locked7,abbrev8,text8,locked8,abbrev9,text9,locked9,abbrev10,text10,locked10,";
		cmd += "abbrev11,text11,locked11,abbrev12,text12,locked12,abbrev13,text13,locked13,abbrev14,text14,locked14,";
		cmd += "abbrev15,text15,locked15) values("+region+",'"+regionName+"','"+name+"',0,"+lock.length+","+indexj+",0";
		for (int i=0; i<16; i++){
			if (i < lock.length){
				cmd += ",'" + abbrev[i]+"'";
				cmd += ",'" + text[i]  +"'";
				cmd += ",'" + lock[i]  +"'";
			}else{
				cmd += ",''";
				cmd += ",''";
				cmd += ",-1";
			}
		}
		cmd += ")";
		
		getWritableDatabase().execSQL(cmd);
	}
	
	/**
	 * 生成Standard ATSC V-Chip Dimensions
	 */
	public void builtinAtscDimensions(){
		getWritableDatabase().execSQL("delete from dimension_table");
		
		/* Add U.S. Rating region 0x1 */
		String[] abbrev0 = {"","None","TV-G","TV-PG","TV-14","TV-MA"};
		String[] text0   = {"","None","TV-G","TV-PG","TV-14","TV-MA"};
		int[]    lock0   = {-1, -1,    0,      0,      0,     0};
		String[] abbrev1 = {"","D","TV-G","TV-PG","TV-14","TV-MA"};
		String[] text1   = {"","D","TV-G","TV-PG","TV-14","TV-MA"};
		int[]    lock1   = {-1, -1,    -1,      0,      0,     -1};
		String[] abbrev2 = {"","L","TV-G","TV-PG","TV-14","TV-MA"};
		String[] text2   = {"","L","TV-G","TV-PG","TV-14","TV-MA"};
		int[]    lock2   = {-1, -1,    -1,      0,      0,      0};
		String[] abbrev3 = {"","S","TV-G","TV-PG","TV-14","TV-MA"};
		String[] text3   = {"","S","TV-G","TV-PG","TV-14","TV-MA"};
		int[]    lock3   = {-1, -1,    -1,      0,      0,      0};
		String[] abbrev4 = {"","V","TV-G","TV-PG","TV-14","TV-MA"};
		String[] text4   = {"","V","TV-G","TV-PG","TV-14","TV-MA"};
		int[]    lock4   = {-1, -1,    -1,      0,      0,      0};
		String[] abbrev5 = {"","TV-Y","TV-Y7"};
		String[] text5   = {"","TV-Y","TV-Y7"};
		int[]    lock5   = {-1,  0,       0};
		String[] abbrev6 = {"","FV","TV-Y7"};
		String[] text6   = {"","FV","TV-Y7"};
		int[]    lock6   = {-1, -1,       0};
		String[] abbrev7 = {"","N/A","G","PG","PG-13","R","NC-17","X","NR"};
		String[] text7   = {"","MPAA Rating Not Applicable","Suitable for AllAges",
		                    "Parental GuidanceSuggested", "Parents Strongly Cautioned",
		                    "Restricted, under 17 must be accompanied by adult",
		                    "No One 17 and Under Admitted","No One 17 and Under Admitted",
		                    "“Not Rated by MPAA"};
		int[]    lock7   = {-1, -1, 0, 0, 0, 0, 0, 0, 0};
		/*Extra for 'All' */
		String[] abbrevall = {"TV-Y","TV-Y7","TV-G","TV-PG","TV-14","TV-MA"};
		String[] textall   = {"TV-Y","TV-Y7","TV-G","TV-PG","TV-14","TV-MA"};
		int[]    lockall   = {0,     0,      0,      0,      0,     0};
		
		insertNewDimension(TVDimension.REGION_US, "US (50 states + possessions)", 
			"Entire Audience",  0, lock0, abbrev0, text0);
		insertNewDimension(TVDimension.REGION_US, "US (50 states + possessions)", 
			"Dialogue",         1, lock1, abbrev1, text1);
		insertNewDimension(TVDimension.REGION_US, "US (50 states + possessions)", 
			"Language",         2, lock2, abbrev2, text2);
		insertNewDimension(TVDimension.REGION_US, "US (50 states + possessions)", 
			"Sex",              3, lock3, abbrev3, text3);
		insertNewDimension(TVDimension.REGION_US, "US (50 states + possessions)", 
			"Violence",         4, lock4, abbrev4, text4);
		insertNewDimension(TVDimension.REGION_US, "US (50 states + possessions)", 
			"Children",         5, lock5, abbrev5, text5);
		insertNewDimension(TVDimension.REGION_US, "US (50 states + possessions)", 
			"Fantasy violence", 6, lock6, abbrev6, text6);
		insertNewDimension(TVDimension.REGION_US, "US (50 states + possessions)", 
			"MPAA",             7, lock7, abbrev7, text7);
		insertNewDimension(TVDimension.REGION_US, "US (50 states + possessions)", 
			"All",             -1, lockall, abbrevall, textall);
		
		/* Add Canadian Rating region 0x2 */
		String[] cabbrev0 = {"E",     "C",       "C8+","G",      "PG","14+","18+"};
		String[] ctext0   = {"Exempt","Children","8+", "General","PG","14+","18+"};
		int[]    clock0   = {0,       0,         0,    0,        0,   0,    0};
		String[] cabbrev1 = {"E",        "G",        "8 ans+","13 ans+","16 ans+","18 ans+"};
		String[] ctext1   = {"Exemptées","Pour tous","8+",    "13+",    "16+",    "18+"};
		int[]    clock1   = {0,          0,          0,       0,        0,        0};
		
		insertNewDimension(TVDimension.REGION_CANADA, "Canada", 
			"Canadian English Language Rating", 0, clock0, cabbrev0, ctext0);
		insertNewDimension(TVDimension.REGION_CANADA, "Canada", 
			"Codes français du Canada",         1, clock1, cabbrev1, ctext1);
	}

	
    /*Load native library*/
    static {
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

		Log.d(TAG, "database version: DB_VERSION "+DB_VERSION+", curVer "+curVer);
		if(curVer != DB_VERSION || !file.exists()){
			create = true;
			if (file.exists()){
				Log.d(TAG, "Database version changed, delete the current database.");
				file.delete();
			}
		}

		if (!file.exists()){
			try{
				Log.d(TAG, "Creating database file ...");
				file.createNewFile();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		
		native_db_setup(file.toString(), create, getWritableDatabase());

		if(create){
			/*Generate builtin data*/
			loadBuiltins();

			pref.edit().putInt(DB_VERSION_FIELD, DB_VERSION).commit();
		}
	}

	public void importFromXml(String inputXmlPath) throws Exception{
		TVDBTransformer.transform(TVDBTransformer.XML_TO_DB, 
			getWritableDatabase(), inputXmlPath);
	}

	public void exportToXml(String outputXmlPath) throws Exception{
		TVDBTransformer.transform(TVDBTransformer.DB_TO_XML, 
			getReadableDatabase(), outputXmlPath);
	}
	
	/**
	 *加载需要内置的数据到数据库
	 */
	public void loadBuiltins(){
		try{
			/* load database from xml */
			Log.d(TAG, "Loading default database from " + DEFAULT_DB_PATH + "...");
			importFromXml(DEFAULT_DB_PATH);
		}catch (Exception e){
			e.printStackTrace();
		}
		
		/* builtin default ATSC V-Chip dimensions */
		Log.d(TAG, "Generating builtin v-chip dimensions...");
		builtinAtscDimensions();
	}

	public static void sync(){
		native_db_sync();
	}

	@Override
	public void onCreate(SQLiteDatabase db){
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
	}
}

