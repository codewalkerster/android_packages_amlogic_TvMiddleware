package com.amlogic.tvutil;

import android.database.Cursor;
import android.content.Context;
import android.util.Log;
import com.amlogic.tvdataprovider.TVDataProvider;

/**
 *TV Group相关信息.
 *Group对应DTV中的一个节目分组
 */
public class TVGroup{
	private static final String TAG="TVGroup";
	private Context context;
	private int id;
	private String name;

	/**
	*设置节目分组ID
	*@param id 节目分组ID
	*/
	public void setID(int id){
		this.id=id;
	}

	/**
	*取得节目分组ID
	*@return 返回节目分组ID
	*/
	public int getID(){
		return this.id;
	}
	
	/**
	*设置节目分组名称
	*@param name 节目分组名称
	*/
	public void setName(String name){
		this.name = name;
	} 

	/**
	*取得节目分组名称
	*@return 返回节目分组名称
	*/	
	public String getName(){
		return this.name;
	}	

	public TVGroup(){
	}
	
	private TVGroup(Context context, Cursor c){
		int col;

		this.context = context;

		col = c.getColumnIndex("db_id");
		this.id = c.getInt(col);

		col = c.getColumnIndex("name");
		this.name = c.getString(col);
	}

	/**
	*取得所有节目分组信息
	*@param context 当前context
	*@param no_skip 跳过标志
	*@return 返回节目分组信息组
	*/
	public static TVGroup[] selectByGroup(Context context, boolean no_skip){
		TVGroup p[] = null;
		boolean where = false;
		String cmd = "select * from grp_table ";
		
		Cursor c = context.getContentResolver().query(TVDataProvider.RD_URL,
				null,
				cmd,
				null, null);
		if(c != null){
			if(c.moveToFirst()){
				int id = 0;
				p = new TVGroup[c.getCount()];
				do{				
					p[id++] =new TVGroup(context,c);
				}while(c.moveToNext());
			}
			c.close();
		}

		return p;
	}
	
	/**
	*添加节目分组
	*@param context 当前context
	*@param group_name 节目分组名称
	*/
	public static void addGroup(Context context,String group_name){
		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"insert into grp_table (name) values ("+ group_name +")",
				null, null);
		if(c != null){
			c.close();
		}
	}

	/**
	*删除节目分组
	*@param context 当前context
	*@param id 节目分组ID
	*/
	public static void deleteGroup(Context context,int id){

		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"delete from grp_table where db_id = "+ id,
				null, null);
		if(c != null){
			c.close();
		}
	}	

	/**
	*编辑节目分组
	*@param context 当前context
	*@param id 节目分组ID
	*@param name 节目分组名称
	*/
	public static void editGroup(Context context,int id, String name){
		Cursor c = context.getContentResolver().query(TVDataProvider.WR_URL,
				null,
				"update grp_table set name= "+name+" where db_id = "+ id,
				null, null);
		if(c != null){
			c.close();
		}
	}	
	
}
