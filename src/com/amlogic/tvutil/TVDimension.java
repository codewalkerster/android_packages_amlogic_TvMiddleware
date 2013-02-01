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
	private int graduatedScale;
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
		
		col = c.getColumnIndex("graduated_scale");
		this.graduatedScale = c.getInt(col);
		
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
	
	/**
	 *根据记录ID取得对应的TVDimension
	 *@param context 当前Context
	 *@param ratingRegionID rating region ID
	 *@param index RRT中对应的index_j
	 *@return 返回对应的TVDimension对象
	 */
	public static TVDimension[] selectUSDownloadable(Context context){
		TVDimension[] d = null;
		String cmd = "select * from dimension_table where rating_region >= 5";
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null, cmd, null, null);
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
	 *判断指定value是否需要block
	 *@param context 当前Context
	 *@param ratingRegionID rating region ID
	 *@param dimensionIndex RRT中对应的index_j
	 *@param valueIndex RRT中对应的rating_value
	 *@return 是否block
	 */
	public static boolean isBlocked(Context context, int ratingRegionID, int dimensionIndex, int valueIndex){
		TVDimension dm = selectByIndex(context, ratingRegionID, dimensionIndex);
		if (dm != null){
			return (dm.getLockStatus(valueIndex) == 1);
		}
		
		return false;
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
	 *取得graduated scale标志
	 *@return 返回graduated scale标志
	 */
	public int getGraduatedScale(){
		return graduatedScale;
	}
	
	/**
	 *取得该dimension的所有values的加锁状态
	 *@return 返回所有values的加锁状态，0-未加锁，-1-无效值，即不能对该项进行设置，其他-已加锁
	 */
	public int[] getLockStatus(){
		if (lockValues.length > 1){
			int[] l = new int[lockValues.length - 1];
			System.arraycopy(lockValues, 1, l, 0, l.length);
			return l;
		}else{
			return null;
		}
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
		/* the first rating_value must be not visible to user */
		if (abbrevValues.length > 1){
			String[] a = new String[abbrevValues.length - 1];
			System.arraycopy(abbrevValues, 1, a, 0, a.length);
			return a;
		}else{
			return null;
		}
	}

	/**
	 *取得该dimension的所有values的value text
	 *@return 返回所有values的value text
	 */
	public String[] getText(){
		if (textValues.length > 1){
			String[] t = new String[textValues.length - 1];
			System.arraycopy(textValues, 1, t, 0, t.length);
			return t;
		}else{
			return null;
		}
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
		if (status == null || status.length != (lockValues.length-1)){
			Log.d(TAG, "Cannot set lock status, invalid param");
			return;
		}
		
		for (int i=0; i<status.length; i++){
			setLockStatus(i+1, status[i]);
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
