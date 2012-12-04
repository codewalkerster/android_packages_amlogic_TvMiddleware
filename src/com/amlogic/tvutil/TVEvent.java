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
	private int end;
	private int dvbContent;
	private int dvbViewAge;

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
		this.start = c.getInt(col);

		col = c.getColumnIndex("end");
		this.end = c.getInt(col);

		col = c.getColumnIndex("nibble_level");
		this.dvbContent = c.getInt(col);

		col = c.getColumnIndex("parental_rating");
		this.dvbViewAge = c.getInt(col);
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
}
