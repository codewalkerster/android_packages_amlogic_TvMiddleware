package com.amlogic.tvutil;

import java.util.Date;

/**
 *TV 事件
 *对应每个Program一个时段的节目
 */
public class TVEvent{
	private int id;
	private int dvbEventID;
	private String name;
	private String description;
	private TVProgram program;
	private Date start;
	private int duration;
	private int dvbContent;
	private int dvbViewAge;

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
	public Date getStartTime(){
		return start;
	}

	/**
	 *取得事件长度，单位为秒
	 *@return 返回事件长度
	 */
	public int getDuration(){
		return duration;
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
