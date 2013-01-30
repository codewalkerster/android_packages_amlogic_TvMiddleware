package com.amlogic.tvutil;

import android.database.Cursor;
import android.content.Context;
import android.util.Log;
import com.amlogic.tvdataprovider.TVDataProvider;

/**
 *TV ATSC rating dimension
 */
public class TVDimension{
	private static final String TAG="TVDimension";

	/**Rating regions*/
	public static final int REGION_US = 0x1;
	public static final int REGION_CANADA = 0x2;
	public static final int REGION_TAIWAN = 0x3;
	public static final int REGION_SOUTHKOREA = 0x4;
	
	private Context context;
	private int id;
	private int indexj;
	private int ratingRegion;
	private int[] lockValues;
	private String name;
	private String ratingRegionName;
	private String[] abbrevValues;
	private String[] textValues;

	TVDimension(Context context, Cursor c){
		this.context = context;

		int col;

		col = c.getColumnIndex("db_id");
		this.id = c.getInt(col);

		col = c.getColumnIndex("index_j");
		this.indexj = c.getInt(col);
		
		col = c.getColumnIndex("rating_region");
		this.ratingRegion = c.getInt(col);

		col = c.getColumnIndex("name");
		this.name = c.getString(col);
		
		col = c.getColumnIndex("rating_region_name");
		this.ratingRegionName = c.getString(col);
		
		col = c.getColumnIndex("values_defined");
		int valuesDefined = c.getInt(col);
		this.lockValues = new int[valuesDefined];
		this.abbrevValues = new String[valuesDefined];
		this.textValues = new String[valuesDefined];
		for (int i=0; i<valuesDefined; i++){
			col = c.getColumnIndex("abbrev"+i);
			this.abbrevValues[i] = c.getString(col);
			col = c.getColumnIndex("text"+i);
			this.textValues[i] = c.getString(col);
			col = c.getColumnIndex("locked"+i);
			this.lockValues[i] = c.getInt(col);
		}
	}
	
	private static void add(Context context, int region, String regionName, String name, 
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
		
		context.getContentResolver().query(TVDataProvider.WR_URL,
				null, cmd, null, null);
	}

	/**
	 *根据记录ID取得对应的TVDimension
	 *@param context 当前Context
	 *@param id 记录ID
	 *@return 返回ID对应的TVDimension对象
	 */
	public static TVDimension selectByID(Context context, int id){
		TVDimension e = null;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select * from dimension_table where evt_table.db_id = " + id,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				e = new TVDimension(context, c);
			}
			c.close();
		}

		return e;
	}
	
	/**
	 *根据记录ID取得对应的TVDimension
	 *@param context 当前Context
	 *@param ratingRegionID rating region ID
	 *@return 返回ID对应的TVDimension对象
	 */
	public static TVDimension[] selectByRatingRegion(Context context, int ratingRegionID){
		TVDimension[] d = null;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select * from dimension_table where rating_region = " + ratingRegionID,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				d = new TVDimension[c.getCount()];
				do{
					d[id++] = new TVDimension(context, c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return d;
	}
	
	/**
	 *根据记录ID取得对应的TVDimension
	 *@param context 当前Context
	 *@param ratingRegionID rating region ID
	 *@param index RRT中对应的index_j
	 *@return 返回对应的TVDimension对象
	 */
	public static TVDimension selectByIndex(Context context, int ratingRegionID, int index){
		TVDimension d = null;
		String cmd = "select * from dimension_table where rating_region = " + ratingRegionID;
		cmd += " and index_j=" + index;
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null, cmd, null, null);
		if(c != null){
			if(c.moveToFirst()){
				d = new TVDimension(context, c);
			}
			c.close();
		}

		return d;
	}
	
	/**
	 *根据记录ID取得对应的TVDimension
	 *@param context 当前Context
	 *@param ratingRegionID rating region ID
	 *@param index RRT中对应的index_j
	 *@return 返回对应的TVDimension对象
	 */
	public static TVDimension selectByName(Context context, int ratingRegionID, String dimensionName){
		TVDimension d = null;
		String cmd = "select * from dimension_table where rating_region = " + ratingRegionID;
		cmd += " and name='" + dimensionName + "'";
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null, cmd, null, null);
		if(c != null){
			if(c.moveToFirst()){
				d = new TVDimension(context, c);
			}
			c.close();
		}

		return d;
	}
	
	public static boolean isBlocked(Context context, int ratingRegionID, int dimensionIndex, int valueIndex){
		TVDimension dm = selectByIndex(context, ratingRegionID, dimensionIndex);
		if (dm != null){
			return (dm.getLockStatus(valueIndex) == 1);
		}
		
		return false;
	}
	
	/**
	 *恢复成默认数据
	 */
	public static void restore(Context context){
		String cmd = "delete from dimension_table";
		context.getContentResolver().query(TVDataProvider.WR_URL,
				null, cmd, null, null);
		
		/* Add U.S. Rating region 0x1 */
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
		
		add(context, REGION_US, "US (50 states + possessions)", "Dialogue",         1, lock1, abbrev1, text1);
		add(context, REGION_US, "US (50 states + possessions)", "Language",         2, lock2, abbrev2, text2);
		add(context, REGION_US, "US (50 states + possessions)", "Sex",              3, lock3, abbrev3, text3);
		add(context, REGION_US, "US (50 states + possessions)", "Violence",         4, lock4, abbrev4, text4);
		add(context, REGION_US, "US (50 states + possessions)", "Fantasy violence", 6, lock6, abbrev6, text6);
		add(context, REGION_US, "US (50 states + possessions)", "MPAA",             7, lock6, abbrev6, text6);
		add(context, REGION_US, "US (50 states + possessions)", "All",             -1, lockall, abbrevall, textall);
		
		/* Add Canadian Rating region 0x2 */
		String[] cabbrev0 = {"E",     "C",       "C8+","G",      "PG","14+","18+"};
		String[] ctext0   = {"Exempt","Children","8+", "General","PG","14+","18+"};
		int[]    clock0   = {0,       0,         0,    0,        0,   0,    0};
		String[] cabbrev1 = {"E",        "G",        "8 ans+","13 ans+","16 ans+","18 ans+"};
		String[] ctext1   = {"Exemptées","Pour tous","8+",    "13+",    "16+",    "18+"};
		int[]    clock1   = {0,          0,          0,       0,        0,        0};
		
		add(context, REGION_CANADA, "Canada", "Canadian English Language Rating", 0, clock0, cabbrev0, ctext0);
		add(context, REGION_CANADA, "Canada", "Codes français du Canada",         1, clock1, cabbrev1, ctext1);
	}

	/**
	 *取得事件的ID
	 *@return 返回事件的ID
	 */
	public int getID(){
		return id;
	}

	/**
	 *取得 rating region ID
	 *@return 返回 rating region ID
	 */
	public int getRatingRegion(){
		return ratingRegion;
	}

	/**
	 *取得 rating region 名称
	 *@return 返回 rating region 名称
	 */
	public String getRatingRegionName(){
		return ratingRegionName;
	}
	/**
	 *取得Dimension名称
	 *@return 返回Dimension名称
	 */
	public String getName(){
		return name;
	}

	/**
	 *取得该dimension的所有values的加锁状态
	 *@return 返回所有values的加锁状态，0-未加锁，-1-无效值，即不能对该项进行设置，其他-已加锁
	 */
	public int[] getLockStatus(){
		return lockValues;
	}
	
	/**
	 *取得该dimension的指定value的加锁状态
	 *@param valueIndex value索引
	 *@return 返回指定value的加锁状态，0-未加锁，-1-无效值，即不能对该项进行设置，其他-已加锁
	 */
	public int getLockStatus(int valueIndex){
		if (valueIndex >= lockValues.length)
			return -1;
		else
			return lockValues[valueIndex];
	}
	
	/**
	 *取得该dimension的指定几个values的加锁状态
	 *@param abrrevs 需要获取的value的abbrev集合
	 *@return 返回指定values的加锁状态，0-未加锁，-1-无效值，即不能对该项进行设置，其他-已加锁
	 */
	public int[] getLockStatus(String[] abbrevs){
		int l[] = null;
		
		if (abbrevs != null){
			l = new int[abbrevs.length];
			for (int i=0; i<abbrevs.length; i++){
				l[i] = -1;
				for (int j=0; j<abbrevValues.length; j++){
					if (abbrevs[i].equals(abbrevValues[j])){
						l[i] = lockValues[j];
						break;
					}
				}
			}
		}
		
		return l;
	}

	/**
	 *取得该dimension的所有values的abbrev text
	 *@return 返回所有values的abbrev text
	 */
	public String[] getAbbrev(){
		return abbrevValues;
	}

	/**
	 *取得该dimension的所有values的value text
	 *@return 返回所有values的value text
	 */
	public String[] getText(){
		return textValues;
	}
	
	/**
	 *设置指定value的加锁状态
	 *@param valueIndex value索引
	 *@param status 加锁状态
	 */
	public void setLockStatus(int valueIndex, int status){
		if (valueIndex >= lockValues.length)
			return;
		if (lockValues[valueIndex] != -1 && lockValues[valueIndex] != status){
			lockValues[valueIndex] = status;
			String cmd = "update dimension_table set locked" + valueIndex;
			cmd += "=" + status + " where db_id = " + id;
			context.getContentResolver().query(TVDataProvider.WR_URL,
				null, cmd, null, null);
		}
	}
	
	/**
	 *设置该dimension所有values的加锁状态
	 *@param status 加锁状态
	 */
	public void setLockStatus(int[] status){
		if (status == null || status.length != lockValues.length){
			Log.d(TAG, "Cannot set lock status, invalid param");
			return;
		}
		
		for (int i=0; i<status.length; i++){
			setLockStatus(i, status[i]);
		}
	}
	
	/**
	 *设置指定values的加锁状态
	 *@param abbrevs abbrev集合
	 *@param locks 需要修改的与abbrev对应的加锁状态集合
	 */
	public void setLockStatus(String[] abbrevs, int[] locks){
		if (abbrevs == null || locks == null)
			return;
		if (abbrevs.length != locks.length){
			Log.d(TAG, "Invalid abbrevs or locks, length must be equal");
			return;
		}
		for (int i=0; i<abbrevs.length; i++){
			for (int j=0; j<abbrevValues.length; j++){
				if (abbrevs[i].equals(abbrevValues[j])){
					setLockStatus(j, locks[i]);
					break;
				}
			}
		}
	}
}
