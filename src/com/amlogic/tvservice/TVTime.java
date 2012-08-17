package com.amlogic.tvservice;

import java.util.Date;

/**
 *TV时间管理
 */
public class TVTime{
	private long diff = 0;

	/**
	 *创建时间管理器
	 */
	TVTime(){
		diff = 0;
	}

	/**
	 *设定当前时间
	 *@param date 当前时间
	 */
	synchronized void setTime(Date date){
		Date sys = new Date();

		diff = date.getTime() - sys.getTime();
	}

	/**
	 *取得当前时间
	 *@return 返回当前时间
	 */
	synchronized Date getTime(){
		Date sys = new Date();
		long time = sys.getTime() + diff;

		return new Date(time);
	}
}

