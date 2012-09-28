package com.amlogic.tvutil;

import android.util.Log;
import android.os.Parcel;
import android.os.Parcelable;

/**
 *搜索参数
 */
public class TVScanParams implements Parcelable {
	/** General TV Scan Mode */
	public static final int TV_MODE_ATV = 0;	// Only search ATV
	public static final int TV_MODE_DTV = 1;	// Only search DTV
	public static final int TV_MODE_ADTV = 2;	// A/DTV will share a same frequency list, like ATSC
	/** DTV scan mode */
	public static final int DTV_MODE_AUTO   = 1;
	public static final int DTV_MODE_MANUAL = 2;
	public static final int DTV_MODE_ALLBAND= 3;
	public static final int DTV_MODE_BLIND  = 4;
	
	/** ATV scan mode */
	public static final int ATV_MODE_AUTO   = 1;
	public static final int ATV_MODE_MANUAL = 2;

	private int mode;
	private int fendID;
	/** DTV parameters */
	private int dtvMode;
	private int tsSourceID;
	private TVChannelParams startParams;
	/** ATV parameters */
	private int atvMode;
	private int startFreq;
	private int direction;

	public static final Parcelable.Creator<TVScanParams> CREATOR = new Parcelable.Creator<TVScanParams>(){
		public TVScanParams createFromParcel(Parcel in) {
			return new TVScanParams(in);
		}
		public TVScanParams[] newArray(int size) {
			return new TVScanParams[size];
		}
	};

	public void readFromParcel(Parcel in){
		mode   = in.readInt();
		fendID = in.readInt();
		if (mode == TV_MODE_DTV || mode == TV_MODE_ADTV) {
			dtvMode = in.readInt();
			tsSourceID = in.readInt();
			if(dtvMode == DTV_MODE_MANUAL)
				startParams = new TVChannelParams(in);
		}  else if (mode == TV_MODE_ATV) {
			atvMode = in.readInt();
			startFreq = in.readInt();
			direction = in.readInt();
		}
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(mode);
		dest.writeInt(fendID);
		if (mode == TV_MODE_DTV || mode == TV_MODE_ADTV) {
			dest.writeInt(dtvMode);
			dest.writeInt(tsSourceID);
			if(dtvMode == DTV_MODE_MANUAL)
				startParams.writeToParcel(dest, flags);
		} else if (mode == TV_MODE_ATV) {
			dest.writeInt(atvMode);
			dest.writeInt(startFreq);
			dest.writeInt(direction);
		}
		
	}

	public TVScanParams() {

	}
	
	public TVScanParams(Parcel in){
		readFromParcel(in);
	}


	public TVScanParams(int mode){
		this.mode = mode;
	}

	
	public int getTvMode() {
		return mode;
	}

	public int getDtvMode() {
		return dtvMode;
	}

	public int getAtvMode() {
		return atvMode;
	}

	public int getTsSourceID() {
		return tsSourceID;
	}

	/**
	 *创建新的搜索参数
	 *@param sp 原始参数
	 */
	public TVScanParams(TVScanParams sp){
		mode = sp.mode;
		dtvMode = sp.dtvMode;
		fendID = sp.fendID;
		tsSourceID = sp.tsSourceID;
		startParams = sp.startParams;
		atvMode = sp.atvMode;
		startFreq = sp.startFreq;
		direction = sp.direction;
	}

	/**
	 *创建手动搜索参数
	 *@param fendID 前端设备参数
	 *@param params 要搜索的频点参数
	 *@return 返回新创建的搜索参数
	 */
	public static TVScanParams dtvManualScanParams(int fendID, TVChannelParams params){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);

		sp.dtvMode = DTV_MODE_MANUAL;
		sp.fendID = fendID;
		sp.startParams = params;

		return sp;
	}

	/**
	 *创建自动搜索参数
	 *@param fendID 前端设备参数
	 *@param mainParams main frequency contains NIT
	 *@return 返回新创建的搜索参数
	 */
	public static TVScanParams dtvAutoScanParams(int fendID, TVChannelParams mainParams){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);

		sp.dtvMode = DTV_MODE_AUTO;
		sp.fendID = fendID;
		sp.startParams = mainParams;

		return sp;
	}

	/**
	 *创建盲搜参数
	 *@param fendID 前端设备参数
	 *@param tsSourceID TS输入源ID
	 *@return 返回新创建的搜索参数
	 */
	public static TVScanParams dtvBlindScanParams(int fendID, int tsSourceID){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);

		sp.dtvMode = DTV_MODE_BLIND;
		sp.fendID = fendID;
		sp.tsSourceID  = tsSourceID;

		return sp;
	}

	/**
	 * Get DTV allband scan mode params
	 *@param fendID frontend device number
	 *@param tsSourceID frontend type
	 *@return the new TVScanParams object
	 */
	public static TVScanParams dtvAllbandScanParams(int fendID, int tsSourceID){
		TVScanParams sp = new TVScanParams(TV_MODE_DTV);

		sp.dtvMode = DTV_MODE_ALLBAND;
		sp.fendID = fendID;
		sp.tsSourceID  = tsSourceID;

		return sp;
	}

	/**
	 * Get ATV manual scan mode params
	 *@param fendID frontend device number
	 *@param startFreq the start frequency 
	 *@param direction direction to search
	 *@return the new TVScanParams object
	 */
	public static TVScanParams atvManualScanParams(int fendID, int startFreq, int direction){
		TVScanParams sp = new TVScanParams(TV_MODE_ATV);

		sp.atvMode = ATV_MODE_MANUAL;
		sp.fendID = fendID;
		sp.startFreq= startFreq;
		sp.direction = direction;

		return sp;
	}

	/**
	 * Get ATV auto scan mode params
	 *@param fendID frontend device number
	 *@return the new TVScanParams object
	 */
	public static TVScanParams atvAutoScanParams(int fendID){
		TVScanParams sp = new TVScanParams(TV_MODE_ATV);

		sp.atvMode = ATV_MODE_AUTO;
		sp.fendID = fendID;

		return sp;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVScanParams> getCreator() {
		return CREATOR;
	}
}

