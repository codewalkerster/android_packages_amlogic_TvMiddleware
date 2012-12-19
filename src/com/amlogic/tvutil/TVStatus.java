package com.amlogic.tvutil;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 *TV状态
 */
public class TVStatus implements Parcelable{
	public int programType;
	public TVProgramNumber programNo;
	public int programID;

	public static final Parcelable.Creator<TVStatus> CREATOR = new Parcelable.Creator<TVStatus>(){
		public TVStatus createFromParcel(Parcel in) {
			return new TVStatus(in);
		}
		public TVStatus[] newArray(int size) {
			return new TVStatus[size];
		}
	};

	public void readFromParcel(Parcel in){
		programType = in.readInt();
		programID = in.readInt();
		programNo = new TVProgramNumber(in);
	}

	public void writeToParcel(Parcel dest, int flag){
		dest.writeInt(programType);
		dest.writeInt(programID);
		programNo.writeToParcel(dest, flag);
	}

	public TVStatus(Parcel in){
		readFromParcel(in);
	}

	public TVStatus(){
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVStatus> getCreator() {
		return CREATOR;
	}
}

