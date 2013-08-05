package com.amlogic.tvutil;

import android.database.Cursor;
import android.content.Context;
import com.amlogic.tvdataprovider.TVDataProvider;

/**
 *TV 事件
 *对应每个Program一个时段的节目
 */
public class TVEvent{
	private Context context;
	private int id;
	private int dvbEventID;
	private String name;
	private String description;
	private TVProgram program;
	private long start;
	private long end;
	private int dvbContent;
	private int dvbViewAge;
	private int sub_flag;
	private String descr=null;
	private String ext_descr=null;
	private TVDimension.VChipRating[] vchipRatings=null;

	TVEvent(Context context, Cursor c){
		this.context = context;

		int col;

		col = c.getColumnIndex("db_id");
		this.id = c.getInt(col);

		col = c.getColumnIndex("event_id");
		this.dvbEventID = c.getInt(col);

		col = c.getColumnIndex("name");
		this.name = c.getString(col);

		col = c.getColumnIndex("start");
		this.start = (long)c.getInt(col) * 1000;

		col = c.getColumnIndex("end");
		this.end = (long)c.getInt(col) * 1000;

		col = c.getColumnIndex("nibble_level");
		this.dvbContent = c.getInt(col);

		col = c.getColumnIndex("parental_rating");
		this.dvbViewAge = c.getInt(col);

		col = c.getColumnIndex("sub_flag");
		this.sub_flag = c.getInt(col);
		
		col = c.getColumnIndex("db_srv_id");
		int programID = c.getInt(col);
		this.program = TVProgram.selectByID(context, programID);
		
		col = c.getColumnIndex("rrt_ratings");
		String rrtRatings = c.getString(col);
		String[] ratings = rrtRatings.split(",");
		if (ratings != null && ratings.length > 0){
			vchipRatings = new TVDimension.VChipRating[ratings.length];
			TVDimension dm = new TVDimension();
			for (int i=0; i<ratings.length; i++){
				String[] rating = ratings[i].split(" ");
				if (rating.length >= 3){
					vchipRatings[i] = dm.new VChipRating(
				                  Integer.parseInt(rating[0]),
				                  Integer.parseInt(rating[1]),
				                  Integer.parseInt(rating[2]));
				}else{
					/* Actually, this must NOT be true */
					vchipRatings[i] = null;
				}
			}
		}
	}

	/**
	 *根据记录ID取得对应的TVEvent
	 *@param context 当前Context
	 *@param id 记录ID
	 *@return 返回ID对应的TVEvent对象
	 */
	public static TVEvent selectByID(Context context, int id){
		TVEvent e = null;

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select * from evt_table where evt_table.db_id = " + id,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				e = new TVEvent(context, c);
			}
			c.close();
		}

		return e;
	}

	/**
	 *根据节目记录ID删除对应的TVEvent
	 *@param context 当前Context
	 *@param db_srv_id 节目记录ID
	 */
	public static void tvEventDelBySrvID(Context context, int db_srv_id){

		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"delete from evt_table where evt_table.db_srv_id = " + db_srv_id,
				null, null);
		if(c != null){
			c.close();
		}

		return;
	}	

	/**
	 *取得事件的ID
	 *@return 返回事件的ID
	 */
	public int getID(){
		return id;
	}

	/**
	 *取得事件event ID(DVB)
	 *@return 返回event ID
	 */
	public int getDVBEventID(){
		return dvbEventID;
	}

	/**
	 *取得事件名
	 *@return 返回事件名
	 */
	public String getName(){
		return name;
	}

	/**
	 *取得事件详细描述
	 *@return 返回事件详细描述
	 */
	public String getDescription(){
		return description;
	}

	/**
	 *取得事件所属Program
	 *@return 返回事件所属program
	 */
	public TVProgram getProgram(){
		return program;
	}

	/**
	 *取得事件开始时间
	 */
	public long getStartTime(){
		return start;
	}

	/**
	 *取得事件结束时间
	 */
	public long getEndTime(){
		return end;
	}

	/**
	 *取得事件内容分类(DVB)
	 *@return 返回事件内容分类
	 */
	public int getDVBContent(){
		return dvbContent;
	}

	/**
	 *取得观看年龄(DVB)
	 *@return 返回观看年龄，0表示无年龄限制
	 */
	public int getDVBViewAge(){
		return dvbViewAge;
	}

	/**
	 *取得事件预约状态
	 *@return 事件预约状态
	 */
	public int getSubFlag(){
		return this.sub_flag;
	}

	public void setSubFlag(int f){
		this.sub_flag = f;

		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update evt_table set sub_flag = "+f+" where evt_table.db_id = " + id,
				null, null);
		if(c != null){
			c.close();
		}
	}

	/**
	 *取得事件描述信息
	 *@return 事件描述信息
	 */
	public String getEventDescr(){
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select * from evt_table where evt_table.db_id = " + id,
				null, null);
		if(c != null){
			if(c.moveToFirst()){	
				int col = c.getColumnIndex("descr");
				this.descr = c.getString(col);
			}
			c.close();
		}
		
		return descr;
	}
	
	/**
	 *取得事件扩展描述信息
	 *@return 事件扩展描述信息
	 */
	public String getEventExtDescr(){
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				"select * from evt_table where evt_table.db_id = " + id,
				null, null);
		if(c != null){
			if(c.moveToFirst()){	
				int col = c.getColumnIndex("ext_descr");
				this.ext_descr = c.getString(col);
			}
			c.close();
		}
		return ext_descr;
	}

	/**
	 *取得V-Chip级别信息
	 *@return V-Chip级别信息对象
	 */
	public TVDimension.VChipRating[] getVChipRatings(){
		return vchipRatings;
	}
}
