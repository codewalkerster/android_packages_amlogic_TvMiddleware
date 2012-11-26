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
	 *@param time 当前时间（毫秒单位）
	 */
	synchronized void setTime(long time){
		Date sys = new Date();

		diff = time - sys.getTime();
	}

	/**
	 *取得当前时间
	 *@return 返回当前时间
	 */
	synchronized long getTime(){
		Date sys = new Date();

		return sys.getTime() + diff;
	}
}

