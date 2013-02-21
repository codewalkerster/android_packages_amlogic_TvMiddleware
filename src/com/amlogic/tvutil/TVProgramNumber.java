package com.amlogic.tvutil;

import android.os.Parcel;
import android.os.Parcelable;

public class TVProgramNumber implements Parcelable {
	/**如果没有发现子频道，忽略用户的输入*/
	public static final int MINOR_CHECK_NONE         = 0;
	/**如果没有发现子频道，向上寻找（子频道数字增加）,找到子频道号最大的*/
	public static final int MINOR_CHECK_UP           = 1;
	/**如果没有发现子频道，向下寻找（子频道数字减小）,找到子频道号最小的*/
	public static final int MINOR_CHECK_DOWN         = 2;
	/*如果没有发现子频道，向上寻找，然后找到向上最近的.*/
	public static final int MINOR_CHECK_NEAREST_UP   = 3;
	/*如果没有发现子频道，向下寻找，然后找到向下最近的.*/
	public static final int MINOR_CHECK_NEAREST_DOWN = 4;
	
	private int major;
	private int minor;
	private int minorCheck;
	private boolean atscMode;

	public static final Parcelable.Creator<TVProgramNumber> CREATOR = new Parcelable.Creator<TVProgramNumber>(){
		public TVProgramNumber createFromParcel(Parcel in) {
			return new TVProgramNumber(in);
		}
		public TVProgramNumber[] newArray(int size) {
			return new TVProgramNumber[size];
		}
	};

	/**
	 *创建节目号对象
	 *@param no 节目号
	 */
	public TVProgramNumber(int no){
		this.major = no;
		this.minor = 0;
		this.atscMode = false;
		this.minorCheck = MINOR_CHECK_NONE;
	}

	/**
	 *创建节目号对象
	 *@param no 原始节目号
	 */
	public TVProgramNumber(TVProgramNumber no){
		this.major = no.major;
		this.minor = no.minor;
		this.atscMode = no.atscMode;
		this.minorCheck = MINOR_CHECK_NONE;
	}

	/**
	 *创建节目号对象
	 *@param major 主节目号
	 *@param minor 次节目号
	 */
	public TVProgramNumber(int major, int minor){
		this.major = major;
		this.minor = minor;
		this.atscMode = true;
		this.minorCheck = MINOR_CHECK_NONE;
	}

	/**
	 *创建节目号对象,用于ATSC模式
	 *@param major 主节目号
	 *@param minor 次节目号
	 *@param minorCheck 当major-minor不存在时，自动查找子频道策略，如MINOR_CHECK_UP等
	 */
	public TVProgramNumber(int major, int minor, int minorCheck){
		this.major = major;
		this.minor = minor;
		this.atscMode = true;
		this.minorCheck = minorCheck;
	}

	public TVProgramNumber(Parcel in){
		readFromParcel(in);
	}

	public void readFromParcel(Parcel in){
		major = in.readInt();
		minor = in.readInt();
		atscMode = (in.readInt()!=0);
		minorCheck = in.readInt();
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(major);
		dest.writeInt(minor);
		dest.writeInt(atscMode?1:0);
		dest.writeInt(minorCheck);
	}

	/**
	 *取得节目号
	 *@return 返回节目号
	 */
	public int getNumber(){
		return major;
	}

	/**
	 *取得主节目号(ATSC)
	 *@return 返回节目的主节目号
	 */
	public int getMajor(){
		return major;
	}

	/**
	 *取得次节目号(ATSC)
	 *@return 返回节目的次节目号
	 */
	public int getMinor(){
		return minor;
	}

	/**
	 *是否为ATSC模式
	 *@return 如果是ATSC模式返回true
	 */
	public boolean isATSCMode(){
		return atscMode;
	}
	
	/**
	 *取得子频道号自动查找策略(ATSC)
	 *@return 返回子频道号自动查找策略
	 */
	public int getMinorCheck(){
		return minorCheck;
	}

	/**
	 *检查节目号是否相等
	 *@param no 要比较的节目号
	 *@return true表示相等false表示不相等
	 */
	public boolean equals(TVProgramNumber no){
		if(no == null)
			return false;
		return this.major==no.major && this.minor==no.minor;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVProgramNumber> getCreator() {
		return CREATOR;
	}
}

