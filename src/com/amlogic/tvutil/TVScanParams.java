package com.amlogic.tvutil;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *搜索参数
 */
public class TVScanParams implements Parcelable {
	public static final int MODE_MANUAL = 0;
	public static final int MODE_AUTO   = 1;
	public static final int MODE_BLIND  = 2;
	public static final int MODE_ALLBAND= 3;

	private int mode;
	private int fendID;
	private int tsSourceID;
	private TVChannelParams startParams;

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
		tsSourceID = in.readInt();
		if(mode == MODE_MANUAL)
			startParams = new TVChannelParams(in);
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(mode);
		dest.writeInt(fendID);
		dest.writeInt(tsSourceID);
		if(mode == MODE_MANUAL)
			startParams.writeToParcel(dest, flags);
	}

	public TVScanParams(Parcel in){
		readFromParcel(in);
	}


	public TVScanParams(int mode){
		this.mode = mode;
	}

	/**
	 *创建新的搜索参数
	 *@param sp 原始参数
	 */
	public TVScanParams(TVScanParams sp){
		fendID = sp.fendID;
		tsSourceID = sp.tsSourceID;
		startParams = sp.startParams;
	}

	/**
	 *创建手动搜索参数
	 *@param fendID 前端设备参数
	 *@param tsSourceID TS输入源ID
	 *@param params 要搜索的频点参数
	 *@return 返回新创建的搜索参数
	 */
	static TVScanParams manualScanParams(int fendID, int tsSourceID, TVChannelParams params){
		TVScanParams sp = new TVScanParams(MODE_MANUAL);
		
		sp.fendID = fendID;
		sp.tsSourceID  = tsSourceID;
		sp.startParams = params;

		return sp;
	}

	/**
	 *创建自动搜索参数
	 *@param fendID 前端设备参数
	 *@param tsSourceID TS输入源ID
	 *@return 返回新创建的搜索参数
	 */
	static TVScanParams autoScanParams(int fendID, int tsSourceID){
		TVScanParams sp = new TVScanParams(MODE_AUTO);
		
		sp.fendID = fendID;
		sp.tsSourceID  = tsSourceID;

		return sp;
	}

	/**
	 *创建盲搜参数
	 *@param fendID 前端设备参数
	 *@param tsSourceID TS输入源ID
	 *@return 返回新创建的搜索参数
	 */
	static TVScanParams blindScanParams(int fendID, int tsSourceID){
		TVScanParams sp = new TVScanParams(MODE_BLIND);
		
		sp.fendID = fendID;
		sp.tsSourceID  = tsSourceID;

		return sp;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVScanParams> getCreator() {
		return CREATOR;
	}
}

