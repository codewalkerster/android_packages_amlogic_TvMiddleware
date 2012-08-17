package com.amlogic.tvutil;

import java.lang.UnsupportedOperationException;

/**
 *TV Channel对应模拟电视中的一个频点，数字电视中的一个频点调制的TS流
 */
public class TVChannel{
	private int id;
	private int dvbTSID;
	private int dvbOrigNetID;
	private int fendID;
	private int tsSourceID;
	private TVChannelParams params;

	/**
	 *取得Channel ID
	 *@return 返回Channel ID
	 */
	public int getID(){
		return id;
	}

	/**
	 *取得DVB 传输流ID
	 *@return 返回传输流ID
	 */
	public int getDVBTSID(){
		if(params!=null && !params.isDVBMode())
			throw new UnsupportedOperationException();

		return dvbTSID;
	}

	/**
	 *取得DVB原始网络ID
	 *@return 返回原始网络ID
	 */
	public int getDVBOrigNetID(){
		if(params!=null && !params.isDVBMode())
			throw new UnsupportedOperationException();

		return dvbOrigNetID;
	}

	/**
	 *取得前端设备ID
	 *@return 返回前端设备ID.
	 */
	public int getFrontendID(){
		return fendID;
	}

	/**
	 *取得TS输入源ID
	 *@return 返回前端输入源ID
	 */
	public int getTSSourceID(){
		return tsSourceID;
	}

	/**
	 *取得频道参数
	 *@return 返回频道参数
	 */
	public TVChannelParams getParams(){
		return params;
	}

	/**
	 *判断是否为DVBC模式
	 *@return true表示是DVBC模式，false表示不是DVBC模式
	 */
	public boolean isDVBCMode(){
		if((params!=null) && params.isDVBCMode())
			return true;

		return false;
	}

	/**
	 *判断是否为DVBT模式
	 *@return true表示是DVBT模式，false表示不是DVBT模式
	 */
	public boolean isDVBTMode(){
		if((params!=null) && params.isDVBTMode())
			return true;

		return false;
	}

	/**
	 *判断是否为DVBS模式
	 *@return true表示是DVBS模式，false表示不是DVBS模式
	 */
	public boolean isDVBSMode(){
		if((params!=null) && params.isDVBSMode())
			return true;

		return false;
	}

	/**
	 *判断是否为DVB模式
	 *@return true表示是DVB模式，false表示不是DVB模式
	 */
	public boolean isDVBMode(){
		if((params!=null) && params.isDVBMode())
			return true;

		return false;
	}

	/**
	 *判断是否为ATSC模式
	 *@return true表示是ATSC模式，false表示不是ATSC模式
	 */
	public boolean isATSCMode(){
		if((params!=null) && params.isATSCMode())
			return true;

		return false;
	}

	/**
	 *判断是否为模拟模式
	 *@return true表示是模拟模式，false表示不是模拟模式
	 */
	public boolean isAnalogMode(){
		if((params!=null) && params.isAnalogMode())
			return true;

		return false;
	}
}

