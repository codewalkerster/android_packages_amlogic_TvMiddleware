package com.amlogic.tvutil;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *TV回放参数
 */
public class DTVPlaybackParams implements Parcelable {
	public static final int PLAYBACK_ST_STOPPED  = 0;
	public static final int PLAYBACK_ST_PLAYING  = 1;
	public static final int PLAYBACK_ST_PAUSED   = 2;
	public static final int PLAYBACK_ST_FFFB     = 3;
	public static final int PLAYBACK_ST_EXIT     = 4;

	private String filePath;
	private int status;
	private long currentTime;
	private long totalTime;

	public static final Parcelable.Creator<DTVPlaybackParams> CREATOR = new Parcelable.Creator<DTVPlaybackParams>(){
		public DTVPlaybackParams createFromParcel(Parcel in) {
			return new DTVPlaybackParams(in);
		}
		public DTVPlaybackParams[] newArray(int size) {
			return new DTVPlaybackParams[size];
		}
	};

	public void readFromParcel(Parcel in){
		status = in.readInt();
		currentTime = in.readLong();
		totalTime = in.readLong();
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(status);
		dest.writeLong(currentTime);
		dest.writeLong(totalTime);
	}

	public DTVPlaybackParams(Parcel in){
		readFromParcel(in);
	}
	
	public DTVPlaybackParams(){

	}

	public DTVPlaybackParams(String filePath, long totalTime){
		status = PLAYBACK_ST_STOPPED;
		currentTime = 0;
		this.totalTime = totalTime;
		this.filePath = filePath;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<DTVPlaybackParams> getCreator() {
		return CREATOR;
	}

	/**
	 *获取当前回放状态
	 *@return 当前播放状态，如PLAYBACK_ST_PLAYING等
	 */
	public int getStatus(){
		return status;
	}

	/**
	 *获取当前回放时间
	 *@return 当前播放时间，ms
	 */
	public long getCurrentTime(){
		return currentTime;
	}

	/**
	 *获取当前回放总时间
	 *@return 回放总时间，ms
	 */
	public long getTotalTime(){
		return totalTime;
	}
}

