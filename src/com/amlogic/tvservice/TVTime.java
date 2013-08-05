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

	/**
	 *取得TDT/STT与系统时间的差值
	 *@return 返回差值时间
	 */
	synchronized long getDiffTime(){
		return diff;
	}

	/**
	 *设置TDT/STT与系统时间的差值
	 */
	synchronized void setDiffTime(long diff){
		this.diff = diff;
	}
}

