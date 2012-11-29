package com.amlogic.tvutil;

import android.os.Parcel;
import android.os.Parcelable;

public class TVProgramNumber implements Parcelable {
	private int major;
	private int minor;
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
	}

	/**
	 *创建节目号对象
	 *@param no 原始节目号
	 */
	public TVProgramNumber(TVProgramNumber no){
		this.major = no.major;
		this.minor = no.minor;
		this.atscMode = no.atscMode;
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
	}



	public TVProgramNumber(Parcel in){
		readFromParcel(in);
	}

	public void readFromParcel(Parcel in){
		major = in.readInt();
		minor = in.readInt();
		atscMode = (in.readInt()!=0);
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(major);
		dest.writeInt(minor);
		dest.writeInt(atscMode?1:0);
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

